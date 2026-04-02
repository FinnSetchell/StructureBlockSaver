package com.finndog.client;

import net.minecraft.core.BlockPos;

public class ClientWandData {
    public static volatile BlockPos pos1 = null;
    public static volatile BlockPos pos2 = null;
    public static volatile boolean lastSetWasPos1 = true;
}
