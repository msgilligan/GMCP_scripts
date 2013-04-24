package com.github.abrarsyed.gmcp;

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
}
