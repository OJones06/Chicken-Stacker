package com.mobstacker;

/**
 * Interface mixed into ChickenEntity to expose stack count.
 */
public interface StackableChicken {
    int mobstacker_getStackCount();
    void mobstacker_setStackCount(int count);
}
