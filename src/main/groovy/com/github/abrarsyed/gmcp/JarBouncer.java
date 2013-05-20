package com.github.abrarsyed.gmcp;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import cpw.mods.fml.common.asm.transformers.MCPMerger;

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
		args[4] = "-log=ERROR";
		args[5] = inputDir;
		args[6] = outputDir;

		try
		{
			PrintStream stream = System.out;
			System.setOut(new PrintStream(new File(Constants.DIR_LOGS, "FF.log")));

			ConsoleDecompiler.main(args);
			// -din=0 -rbr=0 -dgs=1 -asc=1 -log=WARN {indir} {outdir}

			System.setOut(stream);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void injector(File input, File output, File config)
	{
		String[] args = new String[]
		{
				input.getPath(),
				output.getPath(),
				config.getPath(),
				new File(Constants.DIR_LOGS, "MCInjector.log").getPath()
		};
		// {input} {output} {conf} {log}
		try
		{
			Class<?> c = Class.forName("MCInjector");
			Method m = c.getMethod("main", new Class[] { String[].class });
			m.invoke(null, new Object[] { args });
		}
		catch (Exception e)
		{
			System.err.println("MCInjector has failed!");
			e.printStackTrace();
		}
	}

	public static void MCPMerger(File client, File server, File config)
	{
		String[] args = new String[3];
		args[0] = config.getPath();
		args[1] = client.getPath();
		args[2] = server.getPath();
		MCPMerger.main(args);
	}
}
