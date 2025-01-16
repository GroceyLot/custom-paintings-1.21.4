package com.cpaintings;

import net.fabricmc.api.ModInitializer;
import com.cpaintings.commands.Painting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomPaintings implements ModInitializer {
	public static final String MOD_ID = "custom-paintings";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		Painting.register();
	}
}