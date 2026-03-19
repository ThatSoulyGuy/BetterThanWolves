package btw.modern;

public interface IProgressUpdate {
    void displayProgressMessage(String message);
    void resetProgressAndMessage(String message);
    void setLoadingProgress(int progress);
}
