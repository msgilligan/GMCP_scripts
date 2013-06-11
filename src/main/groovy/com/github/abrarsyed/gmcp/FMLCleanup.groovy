package com.github.abrarsyed.gmcp

class FMLCleanup
{
	def static final before = /(?m)((case|default).+\r?\n)\r?\n/ // Fixes newline after case before case body
	def static final after = /(?m)\r?\n(\r?\n[ \t]+(case|default))/ // Fixes newline after case body before new case

	public static void updateFile(File f)
	{
		def num = 0;
		def text = f.text

		text.findAll(before) { match, group, words->
			num++
			text = text.replace(match, group)
		}

		text.findAll(after) { match, group, words->
			num++
			text = text.replace(match, group)
		}

		if (num > 0)
			f.write(text)
	}
}
