#!/usr/bin/env python3
"""
Transform btw.modern classes to be fully standalone - no extending btw.api classes.

For each btw.modern class that extends btw.api.X:
1. Read the btw.api.X source to find all fields, methods, constructors
2. Read the btw.modern class
3. Merge: copy missing fields/methods from api into modern
4. Remove "extends btw.api.X" (replace with Object or keep other extends)
5. Replace all "btw.api.X" parameter references with "btw.modern.X"
6. Remove @Override annotations that no longer apply (methods only from api parent)
"""

import os
import re
import sys

MODERN_DIR = "Modern-Common/src/main/java/btw/modern"
API_DIR = "Api/src/main/java/btw/api"

def read_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        return f.read()

def write_file(path, content):
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(content)

def get_api_class_name(modern_content):
    """Extract which btw.api class the modern class extends."""
    m = re.search(r'extends\s+btw\.api\.(\w+)', modern_content)
    if m:
        return m.group(1)
    return None

def get_api_parent_of_api(api_content):
    """Check if a btw.api class itself extends another btw.api class."""
    m = re.search(r'extends\s+btw\.api\.(\w+)', api_content)
    if m:
        return m.group(1)
    return None

def get_full_api_content(api_class_name, visited=None):
    """Get the full content of an API class, including inherited members from its btw.api parents."""
    if visited is None:
        visited = set()
    if api_class_name in visited:
        return ""
    visited.add(api_class_name)

    api_path = os.path.join(API_DIR, api_class_name + ".java")
    if not os.path.exists(api_path):
        return ""

    content = read_file(api_path)

    # Check if this api class extends another api class
    parent = get_api_parent_of_api(content)
    if parent:
        parent_content = get_full_api_content(parent, visited)
        # We'll return both - caller handles dedup
        return parent_content + "\n---SEPARATOR---\n" + content

    return content

def extract_class_body(content):
    """Extract the body of the class (everything between the first { and the last })."""
    # Find the class declaration and its opening brace
    # Handle multi-line class declarations
    brace_count = 0
    start = -1
    for i, c in enumerate(content):
        if c == '{':
            if start == -1:
                start = i + 1
            brace_count += 1
        elif c == '}':
            brace_count -= 1
            if brace_count == 0:
                return content[start:i]
    return ""

def extract_members_text(api_content):
    """Extract the raw member text (fields, methods, constructors) from the api class body."""
    body = extract_class_body(api_content)
    return body.strip()

def extract_imports(content):
    """Extract import statements from a file."""
    imports = []
    for line in content.split('\n'):
        stripped = line.strip()
        if stripped.startswith('import '):
            imports.append(stripped)
    return imports

def get_existing_member_names(content):
    """Get a rough set of method/field names defined in the content."""
    names = set()
    # Match method declarations
    for m in re.finditer(r'(?:public|protected|private|static|\s)+[\w<>\[\].,\s]+\s+(\w+)\s*\(', content):
        names.add(m.group(1))
    # Match field declarations
    for m in re.finditer(r'(?:public|protected|private|static|final|\s)+[\w<>\[\].,\s]+\s+(\w+)\s*[;=]', content):
        names.add(m.group(1))
    return names

def parse_members(body_text):
    """Parse the class body into individual member declarations.
    Returns a list of (type, name, full_text) tuples where type is 'field', 'method', 'constructor', 'other'.
    """
    members = []
    # This is a rough parser - split on top-level declarations
    lines = body_text.split('\n')
    current_member = []
    brace_depth = 0

    for line in lines:
        stripped = line.strip()
        if not stripped:
            if current_member and brace_depth == 0:
                members.append('\n'.join(current_member))
                current_member = []
            continue

        current_member.append(line)
        brace_depth += stripped.count('{') - stripped.count('}')

        if brace_depth <= 0 and (stripped.endswith(';') or stripped.endswith('}')) and current_member:
            members.append('\n'.join(current_member))
            current_member = []
            brace_depth = 0

    if current_member:
        members.append('\n'.join(current_member))

    return members

def get_member_signature(member_text):
    """Extract a simplified signature from a member declaration for dedup purposes."""
    # Remove annotations
    text = re.sub(r'@\w+(\([^)]*\))?\s*', '', member_text)
    # Get the first line (declaration)
    first_line = text.strip().split('\n')[0].strip()
    # For methods, extract name and param types
    m = re.match(r'.*?\s+(\w+)\s*\(([^)]*)\)', first_line)
    if m:
        name = m.group(1)
        params = m.group(2).strip()
        # Simplify param types
        param_types = []
        if params:
            for p in params.split(','):
                p = p.strip()
                parts = p.rsplit(' ', 1)
                if parts:
                    param_types.append(parts[0].strip())
        return ('method', name, tuple(param_types))
    # For fields
    m = re.match(r'.*?\s+(\w+)\s*[;=]', first_line)
    if m:
        return ('field', m.group(1), ())
    return ('other', first_line[:50], ())

def transform_file(modern_path, dry_run=False):
    """Transform a single modern file to be standalone."""
    modern_content = read_file(modern_path)
    api_class_name = get_api_class_name(modern_content)

    if not api_class_name:
        return False, "No btw.api parent found"

    api_path = os.path.join(API_DIR, api_class_name + ".java")
    if not os.path.exists(api_path):
        return False, f"API file not found: {api_path}"

    api_content = read_file(api_path)

    # Step 1: Determine what the modern class should extend instead
    # Check if the api class extends something other than Object
    api_extends_match = re.search(r'class\s+\w+\s+extends\s+([\w.]+)', api_content)
    api_extends = None
    if api_extends_match:
        api_extends_class = api_extends_match.group(1)
        if not api_extends_class.startswith('btw.api.'):
            api_extends = api_extends_class  # Keep non-btw.api parents (like java.util.*)

    # Check if the api class implements interfaces
    api_implements_match = re.search(r'class\s+\w+(?:\s+extends\s+[\w.]+)?\s+implements\s+([\w.,\s]+)\s*\{', api_content)
    api_implements = None
    if api_implements_match:
        api_implements = api_implements_match.group(1).strip()

    # Step 2: Build the list of members from the API class hierarchy
    all_api_content = get_full_api_content(api_class_name)

    # Collect all api members from the hierarchy
    api_members = []
    for segment in all_api_content.split('---SEPARATOR---'):
        segment = segment.strip()
        if not segment:
            continue
        body = extract_class_body(segment)
        if body:
            parsed = parse_members(body)
            api_members.extend(parsed)

    # Step 3: Figure out what the modern class already has
    modern_body = extract_class_body(modern_content)
    modern_members = parse_members(modern_body) if modern_body else []

    # Get signatures of existing modern members
    modern_sigs = set()
    for m in modern_members:
        sig = get_member_signature(m)
        modern_sigs.add(sig)

    # Step 4: Find api members not already in modern
    missing_members = []
    for m in api_members:
        sig = get_member_signature(m)
        if sig not in modern_sigs:
            # Replace btw.api references with btw.modern in the member text
            transformed = m.replace('btw.api.', 'btw.modern.')
            missing_members.append(transformed)
            modern_sigs.add(sig)  # Don't add duplicates

    # Step 5: Modify the modern file content
    new_content = modern_content

    # 5a: Remove "extends btw.api.X" from the class declaration
    # Handle: "extends btw.api.X {" or "extends btw.api.X implements Y {"

    # First handle the class declaration
    # Pattern: class ClassName extends btw.api.X (optional implements) {

    # Check if the modern class has its own implements clause
    modern_class_decl = re.search(
        r'((?:public|abstract|final|\s)+class\s+\w+(?:<[^>]*>)?)\s+extends\s+btw\.api\.\w+(\s+implements\s+[\w.,\s<>]+)?\s*\{',
        new_content
    )

    if not modern_class_decl:
        # Try interface
        modern_class_decl = re.search(
            r'((?:public|abstract|final|\s)+interface\s+\w+(?:<[^>]*>)?)\s+extends\s+btw\.api\.\w+(\s*,\s*[\w.,\s<>]+)?\s*\{',
            new_content
        )
        if modern_class_decl:
            # Interface case
            prefix = modern_class_decl.group(1)
            other_extends = modern_class_decl.group(2) or ""

            # Build new declaration
            if other_extends.strip().startswith(','):
                other_extends = other_extends.strip()[1:].strip()
                new_decl = f"{prefix} extends {other_extends} {{"
            else:
                new_decl = f"{prefix} {{"

            new_content = new_content[:modern_class_decl.start()] + new_decl + new_content[modern_class_decl.end():]
        else:
            return False, "Could not parse class declaration"
    else:
        prefix = modern_class_decl.group(1)
        modern_implements = modern_class_decl.group(2) or ""

        # Build new extends clause
        new_extends = ""
        if api_extends:
            # The api parent extended something non-api - modern should too
            # But check if it's already extended via the modern hierarchy
            new_extends = f" extends {api_extends}"

        # Build new implements clause
        new_implements = modern_implements.strip() if modern_implements else ""
        if api_implements:
            # Transform btw.api references in implements
            api_impl_transformed = api_implements.replace('btw.api.', 'btw.modern.')
            if new_implements:
                # Merge - avoid duplicates
                existing = set(x.strip() for x in new_implements.replace('implements', '').split(','))
                for impl in api_impl_transformed.split(','):
                    impl = impl.strip()
                    if impl and impl not in existing:
                        new_implements += f", {impl}"
            else:
                new_implements = f" implements {api_impl_transformed}"

        new_decl = f"{prefix}{new_extends}{new_implements} {{"
        new_content = new_content[:modern_class_decl.start()] + new_decl + new_content[modern_class_decl.end():]

    # 5b: Insert missing members after the class opening brace
    if missing_members:
        # Find the class body start (first { after class declaration)
        class_body_start = new_content.find('{')
        if class_body_start >= 0:
            insert_point = class_body_start + 1
            missing_text = "\n    // --- Inherited from btw.api." + api_class_name + " ---\n"
            for m in missing_members:
                # Indent properly
                indented = '\n'.join('    ' + line if line.strip() else '' for line in m.split('\n'))
                missing_text += indented + "\n\n"
            new_content = new_content[:insert_point] + missing_text + new_content[insert_point:]

    # 5c: Replace ALL remaining btw.api. references with btw.modern.
    new_content = new_content.replace('btw.api.', 'btw.modern.')

    # 5d: Remove @Override annotations for methods that were overriding btw.api parent methods
    # This is tricky - we'll remove @Override only if the method was overriding the api parent
    # For simplicity, remove all @Override that are on methods which don't have a parent
    # other than Object. We'll be conservative and leave @Override if there's still a parent.
    # Actually, if the class no longer extends anything (or only extends Object),
    # remove all @Override except for Object methods (toString, equals, hashCode, etc.)

    # Check if the class still extends something
    still_extends = bool(re.search(r'extends\s+(?!Object\b)\w+', new_content))

    if not still_extends:
        # Remove @Override except on standard Object methods
        object_methods = {'toString', 'equals', 'hashCode', 'clone', 'finalize'}
        lines = new_content.split('\n')
        new_lines = []
        skip_override = False
        for i, line in enumerate(lines):
            stripped = line.strip()
            if stripped == '@Override':
                # Check next non-empty line for method name
                for j in range(i+1, min(i+5, len(lines))):
                    next_stripped = lines[j].strip()
                    if next_stripped:
                        # Extract method name
                        m = re.search(r'\b(\w+)\s*\(', next_stripped)
                        if m and m.group(1) not in object_methods:
                            skip_override = True
                        break
            if skip_override:
                skip_override = False
                continue  # Skip this @Override line
            new_lines.append(line)
        new_content = '\n'.join(new_lines)

    # 5e: Collect needed imports from API
    api_imports = extract_imports(api_content)
    modern_imports = extract_imports(new_content)
    modern_import_set = set(modern_imports)

    new_imports = []
    for imp in api_imports:
        # Transform btw.api imports to btw.modern
        transformed_imp = imp.replace('btw.api.', 'btw.modern.')
        if transformed_imp not in modern_import_set and 'btw.modern.' not in transformed_imp:
            # Only add non-btw imports (btw.modern classes are in same package)
            new_imports.append(transformed_imp)

    if new_imports:
        # Find where to insert imports (after package statement)
        pkg_match = re.search(r'package\s+[\w.]+;\s*\n', new_content)
        if pkg_match:
            insert_pos = pkg_match.end()
            import_text = '\n'.join(new_imports) + '\n'
            new_content = new_content[:insert_pos] + '\n' + import_text + new_content[insert_pos:]

    if not dry_run:
        write_file(modern_path, new_content)

    return True, f"Transformed (added {len(missing_members)} members from api)"


def main():
    dry_run = '--dry-run' in sys.argv

    # Find all modern files that extend btw.api
    modern_files = []
    for f in sorted(os.listdir(MODERN_DIR)):
        if f.endswith('.java'):
            path = os.path.join(MODERN_DIR, f)
            content = read_file(path)
            if 'extends btw.api.' in content:
                modern_files.append(path)

    print(f"Found {len(modern_files)} files extending btw.api classes")
    if dry_run:
        print("DRY RUN - no files will be modified")

    success = 0
    failed = 0

    for path in modern_files:
        name = os.path.basename(path)
        ok, msg = transform_file(path, dry_run)
        if ok:
            print(f"  OK: {name} - {msg}")
            success += 1
        else:
            print(f"  FAIL: {name} - {msg}")
            failed += 1

    print(f"\nDone: {success} transformed, {failed} failed")


if __name__ == '__main__':
    main()
