package trashsoftware.trashSnooker.recorder;

public class ExtraBlock {
    
    public static final int TYPE_META_MATCH = 1;
    public static final int TYPE_SUB_RULES = 2;
    
    final int blockType;
    final Object blockContent;
    
    ExtraBlock(int blockType, Object blockContent) {
        this.blockType = blockType;
        this.blockContent = blockContent;
    }
}
