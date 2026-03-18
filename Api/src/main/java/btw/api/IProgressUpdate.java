package btw.api;

public interface IProgressUpdate {
    void displayProgressMessage(String message);
    void resetProgressAndMessage(String message);
    void setLoadingProgress(int progress);
}
