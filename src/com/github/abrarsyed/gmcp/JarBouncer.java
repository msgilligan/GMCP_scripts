package com.github.abrarsyed.gmcp;

import java.lang.reflect.Method;

import de.fernflower.main.decompiler.ConsoleDecompiler;

public class JarBouncer
{

	public static void fernFlower(String inputDir, String outputDir)
	{
		String[] args = new String[7];
		args[0] = "-din=0";
		args[1] = "-rbr=0";
		args[2] = "-dgs=1";
		args[3] = "-asc=1";
		args[4] = "-log=WARN";
		args[5] = inputDir;
		args[6] = outputDir;
		ConsoleDecompiler.main(args);
		// -din=0 -rbr=0 -dgs=1 -asc=1 -log=WARN {indir} {outdir}
	}

	public static void retrogauardDeObf(String classPath, String confFile) throws Exception
	{
		String[] args = new String[5];
		args[0] = "-cp";
		args[1] = classPath;
		args[2] = "RetroGaurd";
		args[3] = "-searge";
		args[4] = confFile;
		invokeRG(args);
		// -cp "{classpath}" RetroGuard -searge {conffile}
	}

	public static void retrogauardReObf(String classPath, String confFile) throws Exception
	{
		String[] args = new String[5];
		args[0] = "-cp";
		args[1] = classPath;
		args[2] = "RetroGaurd";
		args[3] = "-notch";
		args[4] = confFile;
		invokeRG(args);
		// -cp "{classpath}" RetroGuard -notch {conffile}
	}

	private static void invokeRG(String[] args) throws Exception
	{
		Method main = Class.forName("RetroGaurd").getDeclaredMethod("main", String[].class);
		main.invoke(null, new Object[] { args });
	}
}
