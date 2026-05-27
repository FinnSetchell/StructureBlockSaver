package com.finndog.utils;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class TickProcessor {
	private TickProcessor() {}

	public interface TickTask {
		boolean tick();
	}

	private static final List<TickTask> TASKS = new ArrayList<>();

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (TASKS.isEmpty()) return;
			Iterator<TickTask> it = TASKS.iterator();
			while (it.hasNext()) {
				TickTask task = it.next();
				if (task.tick()) {
					it.remove();
				}
			}
		});
	}

	public static void submit(TickTask task) {
		TASKS.add(task);
	}
}
