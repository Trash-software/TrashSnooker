package trashsoftware.trashSnooker.core.career;

public class CareerNotPresentException extends RuntimeException {
    
    public CareerNotPresentException() {
        
    }
    
    public CareerNotPresentException(String msg) {
        super(msg);
    }
}
