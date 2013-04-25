package com.github.abrarsyed.gmcp;

import java.io.File;
import java.io.PrintStream;

import net.md_5.specialsource.SpecialSource;
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
			System.setOut(new PrintStream("tmp/logs/FF.log"));

			ConsoleDecompiler.main(args);
			// -din=0 -rbr=0 -dgs=1 -asc=1 -log=WARN {indir} {outdir}

			System.setOut(stream);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void specialSourceDeObf(File inJar, File outJar, File srg)
	{
		try
		{
			SpecialSource.main(new String[] { "-i=" + inJar.getPath(), "-o=" + outJar.getPath(), "-m=" + srg.getPath() });
			System.out.println("WORKED!");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
