package com.gdetotut.jundo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The UndoGroup class is a group of {@link UndoStack} objects.
 * <p>An application often has multiple undo stacks, one for each subject. At the same time,
 * an application usually has one undo action and one redo action, which triggers undo or redo for the active subject.
 * <p>UndoGroup is a group of {@link UndoStack} objects, one of which may be active.
 * It has an {@link #undo} and {@link #redo} methods, which calls {@link UndoStack#undo} and {@link UndoStack#redo}
 * for the active stack.
 * <p>Stacks are added to a group with {@link #add} and removed with {@link #remove}.
 * A stack is implicitly added to a group when it is created with the group as its parent.
 * <p><b>UndoGroup doesn't allow to add 2 stacks with the same subject (compares by address) because
 * it is a logical violation.</b>
 * <p>It is the programmer's responsibility to specify which stack is active by calling {@link UndoStack#setActive},
 * usually when the associated subject "receives focus". The active stack may also be set with {@link #setActive},
 * and is returned by {@link #getActive}.
 */
public class UndoGroup implements Serializable {

    /**
     * Active stack. Can be null if no one stack is active at the moment.
     */
    private UndoStack active;

    /**
     * List of associated stacks.
     */
    private final List<UndoStack> stacks = new ArrayList<>();

    /**
     * Use this method instead of destructor.
     * <p>Ensure all UndoStacks no longer refer to this group when it's time to do it.
     */
    public void clear() {
        for (UndoStack stack : stacks) {
            stack.group = null;
        }
        stacks.clear();
    }

    /**
     * Adds {@link UndoStack} to this group.
     *
     * @param stack stack to be added. Required.
     */
    public void add(UndoStack stack) {
        if (null == stack) {
            throw new NullPointerException("stack");
        } else if (!stacks.contains(stack)) {
            stacks.add(stack);
            if (null != stack.group) {
                stack.group.remove(stack);
            }
            stack.group = this;
        }
    }

    /**
     * Removes stack from this group. If the stack was the active stack in the group,
     * the active stack becomes null.
     *
     * @param stack stack to be removed. Required.
     */
    public void remove(UndoStack stack) {
        if (null == stack) {
            throw new NullPointerException("stack");
        } else {
            if (stack == active) {
                setActive((UndoStack) null);
            }
            stack.group = null;
            stacks.remove(stack);
        }
    }

    /**
     * Returns a list of stacks in this group.
     *
     * @return Stack list.
     */
    public List<UndoStack> getStacks() {
        return stacks;
    }

    /**
     * Sets the active stack of this group to stack.
     * <p>If the stack is not a member of this group, this function does nothing.
     * Synonymous with calling {@link UndoStack#setActive} on stack.
     *
     * @param stack stack to make active or null.
     */
    public void setActive(UndoStack stack) {
        if (active == stack) {
            return;
        }
        active = stack;
    }

    /**
     * Returns the active stack of this group.
     * <p>If none of the stacks are active, or if the group is empty, this function returns null.
     *
     * @return active stack or null.
     */
    public UndoStack getActive() {
        return active;
    }

    /**
     * Calls {@link UndoStack#undo} on the active stack.
     * <p>If none of the stacks are active, or if the group is empty, this function  does nothing.
     */
    public void undo() {
        if (null != active) {
            active.undo();
        }
    }

    /**
     * Calls {@link UndoStack#redo} on the active stack.
     * <p>If none of the stacks are active, or if the group is empty, this function  does nothing.
     */
    public void redo() {
        if (null != active) {
            active.redo();
        }
    }

    /**
     * @return The value of the active stack's {@link UndoStack#canUndo}.
     * <p>If none of the stacks are active, or if the group is empty, this function returns false.
     */
    public boolean canUndo() {
        return null != active && active.canUndo();
    }

    /**
     * @return The value of the active stack's {@link UndoStack#canRedo}.
     * <p>If none of the stacks are active, or if the group is empty, this function returns false.
     */
    public boolean canRedo() {
        return null != active && active.canRedo();
    }

    /**
     * @return The value of the active stack's {@link UndoStack#undoCaption}.
     * <p>If none of the stacks are active, or if the group is empty, this function returns an empty string.
     */
    public String undoCaption() {
        return null != active ? active.undoCaption() : "";
    }

    /**
     * @return The value of the active stack's {@link UndoStack#redoCaption}.
     * <p>If none of the stacks are active, or if the group is empty, this function returns an empty string.
     */
    public String redoCaption() {
        return null != active ? active.redoCaption() : "";
    }

    /**
     * @return The value of the active stack's {@link UndoStack#isClean}.
     * <p>If none of the stacks are active, or if the group is empty, this function returns true.
     */
    public boolean isClean() {
        return null == active || active.isClean();
    }

}
