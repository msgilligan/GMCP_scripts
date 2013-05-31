package com.github.abrarsyed.gmcp

import java.lang.reflect.Method

import cpw.mods.fml.common.asm.transformers.MCPMerger
import de.fernflower.main.decompiler.ConsoleDecompiler
import com.github.abrarsyed.jastyle.ASFormatter
import com.github.abrarsyed.jastyle.OptParser

public class JarBouncer
{

	public static void fernFlower(String inputDir, String outputDir)
	{
		String[] args = new String[7]
		args[0] = "-din=0"
		args[1] = "-rbr=0"
		args[2] = "-dgs=1"
		args[3] = "-asc=1"
		args[4] = "-log=ERROR"
		args[5] = inputDir
		args[6] = outputDir

		try
		{
			PrintStream stream = System.out
			System.setOut(new PrintStream(new File(Constants.DIR_LOGS, "FF.log")))

			ConsoleDecompiler.main(args)
			// -din=0 -rbr=0 -dgs=1 -asc=1 -log=WARN {indir} {outdir}

			System.setOut(stream)
		}
		catch (Exception e)
		{
			e.printStackTrace()
		}
	}

	public static void injector(File input, File output, File config)
	{
		String[] args = new String[4]
		args[0] = input.getPath()
		args[1] = output.getPath()
		args[2] = config.getPath()
		args[3] = new File(Constants.DIR_LOGS, "MCInjector.log").getPath()

		// {input} {output} {conf} {log}
		try
		{
			Class<?> c = Class.forName("MCInjector")
			Method m = c.getMethod("main", String[].class)
			m.invoke(null, [args] as Object[])
		}
		catch (Exception e)
		{
			System.err.println("MCInjector has failed!")
			e.printStackTrace()
		}
	}

	public static void MCPMerger(File client, File server, File config)
	{
		String[] args = new String[3]
		args[0] = config.getPath()
		args[1] = client.getPath()
		args[2] = server.getPath()
		MCPMerger.main(args)
	}

	public static void formatter(File inputDir, File astyleConf)
	{
		try
		{
			ASFormatter formatter = new ASFormatter()
			OptParser parser = new OptParser(formatter)
			
			parser.parseOptionFile(astyleConf)
			formatter.setBreakBlocksMode(true)
						
			
			inputDir.eachFileRecurse {
				if (it.isFile() && it.name.endsWith(".java"))
					formatter.formatFile(it)
			}
			
		}
		catch (Exception e)
		{
			System.err.println("JAStyle has failed!")
			e.printStackTrace()
		}
	}
}
