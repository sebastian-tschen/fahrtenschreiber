package listeners;

@FunctionalInterface
public interface OnCancelledListener {


    void onCancel(Exception error);
}
