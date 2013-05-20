package com.github.abrarsyed.gmcp


public final class Constants
{
	// root dirs
	public static final File	DIR_TEMP		= new File("tmp")
	public static final File	DIR_RESOURCES	= new File("resources")

	// temp dirs
	public static final File	DIR_LOGS		= new File(DIR_TEMP, "logs")
	public static final File	DIR_EXTRACTED	= new File(DIR_TEMP, "extracted")
	public static final File	DIR_CLASSES		= new File(DIR_TEMP, "classes")
	public static final File	DIR_SOURCES		= new File(DIR_TEMP, "sources")
	public static final File 	DIR_MC_JARS 	= new File(DIR_TEMP, "jars")
	public static final File 	DIR_FORGE 	= new File(DIR_TEMP, "forge")

	// jar files
	public static final File	JAR_CLIENT		= new File(DIR_TEMP, "jars/Minecraft_Client.jar")
	public static final File	JAR_SERVER		= new File(DIR_TEMP, "jars/Minecraft_Server.jar")
	public static final File	JAR_MERGED		= new File(DIR_TEMP, "jars/Minecraft.jar")
	public static final File	JAR_DEOBF		= new File(DIR_TEMP, "Minecraft_SS.jar")
	public static final File	JAR_EXCEPTOR	= new File(DIR_TEMP, "Minecraft_EXC.jar")

	// download URLs    versions are in #_#_# form rather than #.#.#
	public static final String URL_MC_JAR = "http://assets.minecraft.net/%s/minecraft.jar"
	public static final String URL_MCSERVER_JAR = "http://assets.minecraft.net/%s/minecraft_server.jar"
	public static final String URL_LIB_ROOT = "http://s3.amazonaws.com/MinecraftDownload/"

	// normal MC version form
	public static final String URL_FORGE = "http://files.minecraftforge.net/minecraftforge/minecraftforge-src-%s-%s.zip"

	// lib names
	public static final LIBRARIES = [
		"lwjgl.jar",
		"lwjgl_util.jar",
		"jinput.jar"
	]

	// natives
	public static final NATIVES = [
		WINDOWS:"windows_natives.jar",
		MAC: "macosx_natives.jar",
		LINUX: "linux_natives.jar"
	]

	// File names...


	static enum OperatingSystem
	{
		WINDOWS, MAC, LINUX
	}
}
