package io.manebot.plugin.matrix;

import io.manebot.database.Database;
import io.manebot.plugin.*;
import io.manebot.plugin.java.*;
import io.manebot.plugin.matrix.command.MatrixCommand;
import io.manebot.plugin.matrix.database.model.MatrixHomeserver;
import io.manebot.plugin.matrix.platform.*;
import io.manebot.plugin.matrix.platform.homeserver.HomeserverManager;

public class Entry implements PluginEntry {
	@Override
	public void instantiate(Plugin.Builder builder) {
		builder.setType(PluginType.FEATURE);

		final Database database = builder.addDatabase("matrix", modelConstructor -> {
			modelConstructor.addDependency(modelConstructor.getSystemDatabase());
			modelConstructor.registerEntity(MatrixHomeserver.class);
		});

		builder.setInstance(HomeserverManager.class, plugin -> new HomeserverManager(database));

		builder.addCommand(
				"matrix",
				future -> new MatrixCommand(
						future.getPlugin(),
						future.getPlugin().getInstance(HomeserverManager.class)
				)
		);

		builder.addPlatform(platformBuilder -> {
			platformBuilder.setId("matrix").setName("Matrix");

			platformBuilder.setConnection(new MatrixPlatformConnection(
					platformBuilder.getPlatform(),
					platformBuilder.getPlugin()
			));
		});
	}
}
