package com.github.abrarsyed.gmcp

import java.util.regex.Matcher

import com.google.common.base.Strings

class FFPatcher
{
	static final MODIFIERS = /public|protected|private|static|abstract|final|native|synchronized|transient|volatile|strict/;
	static final Map<String, String> REG = [

		// Remove trailing whitespace
		trailing : /(?m)[ \t]+$/,

		//Remove repeated blank lines
		newlines: /(?m)^/+System.lineSeparator+/{2,}/,

		modifiers: /(/ + MODIFIERS + /) /,
		list : /, /,

		enum_class: /(?m)^(?<modifiers>(?:(?:/ + MODIFIERS + /) )*)(?<type>enum) (?<name>[\w$]+)(?: implements (?<implements>[\w$.]+(?:, [\w$.]+)*))? \{/+System.lineSeparator+/(?<body>(?:.*?/+System.lineSeparator+/)*?)(?<end>\}/+System.lineSeparator+/+)/,

		enum_entries: /(?m)^ {3}(?<name>[\w$]+)\("(?=name)", [0-9]+(?:, (?<body>.*?))?\)(?<end>(?:;|,)/+System.lineSeparator+/+)/,

		empty_super: /(?m)^ +super\(\);/+System.lineSeparator,

		// strip trailing 0 from doubles and floats to fix decompile differences on OSX
		// 0.0010D => 0.001D
		trailingzero: /(?<value>[0-9]+\.[0-9]*[1-9])0+(?<type>[DdFfEe])/,
	];

	static final Map<String, String> REG_FORMAT = [
		constructor : /(?m)^ {3}(?<modifiers>(?:(?:/ + MODIFIERS + /) )*)%s\((?<parameters>.*?)\)(?: throws (?<throws>[\w$.]+(?:, [\w$.]+)*))? \{(?:(?<empty>\}/+System.lineSeparator+/+)|(?:(?<body>/+System.lineSeparator+/(?:.*?/+System.lineSeparator+/)*?)(?<end> {3}\}/+System.lineSeparator+/+)))/,

		enumVals: "(?m)^ {3}// \\\$FF: synthetic field"+System.lineSeparator+" {3}private static final %s\\[\\] [\\w\$]+ = new %s\\[\\]\\{.*?\\};"+System.lineSeparator,
	];

	def static processDir(File dir)
	{
		dir.eachFile {
			if (it.isDirectory())
				processDir(it)
			else if (it.getPath().endsWith(".java"))
				processFile(it);
		}
	}

	def static processFile(File file)
	{
		def classname = file.getName().split(/\./)[0];
		def text = file.text;

		text.replaceAll(REG["trailing"], "");

		text.findAll(REG["enum_class"])
		{ Matcher match->

			if (classname != match.group('name'))
			{
				println "ERROR PARSING ENUM !!!!! Class Name != File Name";
				return;
			}

			def mods = match.group('modifiers').findAll(REG['modifiers']);
			if (match.group('modifiers') &&  mods.isEmpty())
			{
				println "ERROR PARSING ENUM !!!!! no modifiers!";
				return;
			}

			def interfaces = []
			if (match.group('implements'))
			{
				interfaces = match.group('implements').findAll(REG['list']);
			}

			return processEnum(classname, match.group('type'), mods, interfaces, match.group['body'], match.group['end'])
		};

		text.replaceAll(REG["empty_super"], "");
		text.replaceAll(REG["trailingzero"], "");
		text.replaceAll(REG["newlines"], System.lineSeparator);
		text.replaceAll(REG["trailing"], "");

		file.write(text);
	}

	def static processEnum(classname, classtype, List modifiers, List interfaces, String body, end)
	{
		body.eachMatch(end)
		{ Matcher match ->
			def entryBody = '';
			if (match.group('body'))
			{
				entryBody = "(${match.group('body')})";
			}

			return '   ' + match.group('name') + entryBody + match.group('end');
		};

		def valuesRegex = REG_FORMAT['enumVals'].format(classname, classname);
		body.replaceAll(valuesRegex, "");

		def conRegex = REG_FORMAT['constructor'].format(classname)

		// process constructors
		body.eachMatch(conRegex)
		{ Matcher match ->

			// check modifiers
			def mods = match.group('modifiers').findAll(REG['modifiers']);
			if (match.group('modifiers') &&  mods.isEmpty())
			{
				println "ERROR PARSING ENUM CONSTRUCTOR! !!!!! no modifiers!";
				return;
			}

			def params = [];
			if (match.group('parameters'))
			{
				println "ERROR PARSING ENUM CONSTRUCTOR! !!!!! no modifiers!";
				params = match.group('parameters').split(REG['list']);
				return;
			}

			def exc = [];
			if (match.group('throws'))
			{
				exc = match.group(['throws']).split(REG['list']);
			}

			def methodBody, methodEnd;
			if (!Strings.isNullOrEmpty(match.group('empty')))
			{
				methodBody = '';
				methodEnd = match.group('empty');
			}
			else
			{
				methodBody = match.group('body')
				methodEnd = match.group('end')
			}

			return processConstructor(classname, mods, params, exc, methodBody, methodEnd)
		};

		// rebuild enum
		def out = ''

		if (!modifiers.isEmpty())
		{
			out += modifiers.join(' ')
		}

		out += classtype + ' ' + classname;

		if (!interfaces.isEmpty())
		{
			out += ' implements '+interfaces.join(', ')
		}

		out += "{ ${System.lineSeparator}${body}${end}"

		return out;
	}

	def static processConstructor(classname, List<String> mods, List<String> params, List<String> exc, methodBody, methodEnd)
	{
		if (params.size() >= 2)
		{
			// special case?
			if (params.get(0).startsWith('String ') && params.get(1).startsWith('int '))
			{
				params = params.subList(2, params.size()-1);

				// empty constructor
				if (Strings.isNullOrEmpty(methodBody) && params.isEmpty())
				{
					return '';
				}
			}
			else
			{
				println "invalid initial parameters in enum"
				return
			}
			// ERROR
		}
		else
		{
			println "not enough parameters in enum"
			return
		}

		// reuild constructor

		def out = '   ';
		if (mods)
		{
			out += ' '+mods.join(" ")+' '
		}

		out += classname + '(${params.join(", ")})'

		if (exc)
		{
			out += ' throws ${exc.join(", ")}'
		}

		out += ' {${methodBody}${methodEnd}'
	}
}