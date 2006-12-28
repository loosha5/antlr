/*
 [The "BSD licence"]
 Copyright (c) 2005-2006 Terence Parr
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may not be used to endorse or promote products
    derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.antlr.test;

import org.antlr.Tool;
import org.antlr.codegen.CodeGenerator;
import org.antlr.codegen.ActionTranslatorLexer;
import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.AngleBracketTemplateLexer;
import org.antlr.tool.*;

import java.io.StringReader;
import java.util.List;
import java.util.ArrayList;

/** Check the $x, $x.y attributes.  For checking the actual
 *  translation, assume the Java target.  This is still a great test
 *  for the semantics of the $x.y stuff regardless of the target.
 */
public class TestAttributes extends BaseTest {

	/** Public default constructor used by TestRig */
	public TestAttributes() {
	}

	public void testEscapedLessThanInAction() throws Exception {
		Grammar g = new Grammar();
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		String action = "i<3; '<xmltag>'";
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),0);
		String expecting = action;
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, "<action>");
		actionST.setAttribute("action", rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);
	}

	public void testEscaped$InAction() throws Exception {
		String action = "int \\$n; \"\\$in string\\$\"";
		String expecting = "int $n; \"$in string$\"";
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"@members {"+action+"}\n"+
				"a[User u, int i]\n" +
				"        : {"+action+"}\n" +
				"        ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "a",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),0);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);
	}

	public void testArguments() throws Exception {
		String action = "$i; $i.x; $u; $u.x";
		String expecting = "i; i.x; u; u.x";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"a[User u, int i]\n" +
				"        : {"+action+"}\n" +
				"        ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	/** $x.start refs are checked during translation not before so ANTLR misses
	 the fact that rule r has refs to predefined attributes if the ref is after
	 the def of the method or self-referential.  Actually would be ok if I didn't
	 convert actions to strings; keep as templates.
	 June 9, 2006: made action translation leave templates not strings
	 */
	public void testRefToReturnValueBeforeRefToPredefinedAttr() throws Exception {
		String action = "$x.foo";
		String expecting = "x.foo";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
			"a : x=b {"+action+"} ;\n" +
			"b returns [int foo] : B {$b.start} ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testInvalidArguments() throws Exception {
		String action = "$x";
		String expecting = action;

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"a[User u, int i]\n" +
				"        : {"+action+"}\n" +
				"        ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,
																	 "a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		int expectedMsgID = ErrorManager.MSG_UNKNOWN_SIMPLE_ATTRIBUTE;
		Object expectedArg = "x";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg);
		checkError(equeue, expectedMessage);
	}

	public void testReturnValue() throws Exception {
		String action = "$x.i";
		String expecting = "x";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a returns [int i]\n" +
				"        : 'a'\n" +
				"        ;\n" +
				"b : x=a {"+action+"} ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "b",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testReturnValueWithNumber() throws Exception {
		String action = "$x.i1";
		String expecting = "x";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a returns [int i1]\n" +
				"        : 'a'\n" +
				"        ;\n" +
				"b : x=a {"+action+"} ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "b",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testReturnValues() throws Exception {
		String action = "$i; $i.x; $u; $u.x";
		String expecting = "retval.i; retval.i.x; retval.u; retval.u.x";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"a returns [User u, int i]\n" +
				"        : {"+action+"}\n" +
				"        ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	/* regression test for ANTLR-46 */
	public void testReturnWithMultipleRuleRefs() throws Exception {
		String action1 = "$obj = $rule2.obj;";
		String action2 = "$obj = $rule3.obj;";
		String expecting1 = "obj = rule21;";
		String expecting2 = "obj = rule32;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n" +
			"rule1 returns [ Object obj ]\n" +
			":	rule2 { "+action1+" }\n" +
			"|	rule3 { "+action2+" }\n" +
			";\n"+
			"rule2 returns [ Object obj ]\n"+
			":	foo='foo' { $obj = $foo.text; }\n"+
			";\n"+
			"rule3 returns [ Object obj ]\n"+
			":	bar='bar' { $obj = $bar.text; }\n"+
			";");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		int i = 0;
		String action = action1;
		String expecting = expecting1;
		do {
			ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"rule1",
																		 new antlr.CommonToken(ANTLRParser.ACTION,action),i+1);
			String rawTranslation =
					translator.translate();
			StringTemplateGroup templates =
					new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
			StringTemplate actionST = new StringTemplate(templates, rawTranslation);
			String found = actionST.toString();
			assertEquals(expecting, found);
			action = action2;
			expecting = expecting2;
		} while (i++ < 1);
		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testInvalidReturnValues() throws Exception {
		String action = "$x";
		String expecting = action;

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"a returns [User u, int i]\n" +
				"        : {"+action+"}\n" +
				"        ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		int expectedMsgID = ErrorManager.MSG_UNKNOWN_SIMPLE_ATTRIBUTE;
		Object expectedArg = "x";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg);
		checkError(equeue, expectedMessage);
	}

	public void testTokenLabels() throws Exception {
		String action = "$id; $f; $id.text; $id.getText(); $id.dork " +
						"$id.type; $id.line; $id.pos; " +
						"$id.channel; $id.index;";
		String expecting = "id; f; id.getText(); id.getText(); id.dork " +
						   "id.getType(); id.getLine(); id.getCharPositionInLine(); " +
						   "id.getChannel(); id.getTokenIndex();";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"a : id=ID f=FLOAT {"+action+"}\n" +
				"  ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testRuleLabels() throws Exception {
		String action = "$r.x; $r.start; $r.stop; $r.tree; $a.x; $a.stop;";
		String expecting = "r.x; ((Token)r.start); ((Token)r.stop); ((Object)r.tree); r.x; ((Token)r.stop);";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"a returns [int x]\n" +
				"  :\n" +
				"  ;\n"+
				"b : r=a {###"+action+"!!!}\n" +
				"  ;");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // codegen phase sets some vars we need
		StringTemplate codeST = generator.getRecognizerST();
		String code = codeST.toString();
		String found = code.substring(code.indexOf("###")+3,code.indexOf("!!!"));
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testRuleLabelsWithSpecialToken() throws Exception {
		String action = "$r.x; $r.start; $r.stop; $r.tree; $a.x; $a.stop;";
		String expecting = "r.x; ((MYTOKEN)r.start); ((MYTOKEN)r.stop); ((Object)r.tree); r.x; ((MYTOKEN)r.stop);";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"options {TokenLabelType=MYTOKEN;}\n"+
				"a returns [int x]\n" +
				"  :\n" +
				"  ;\n"+
				"b : r=a {###"+action+"!!!}\n" +
				"  ;");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // codegen phase sets some vars we need

		StringTemplate codeST = generator.getRecognizerST();
		String code = codeST.toString();
		String found = code.substring(code.indexOf("###")+3,code.indexOf("!!!"));
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testForwardRefRuleLabels() throws Exception {
		String action = "$r.x; $r.start; $r.stop; $r.tree; $a.x; $a.tree;";
		String expecting = "r.x; ((Token)r.start); ((Token)r.stop); ((Object)r.tree); r.x; ((Object)r.tree);";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"b : r=a {###"+action+"!!!}\n" +
				"  ;\n" +
				"a returns [int x]\n" +
				"  : ;\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // codegen phase sets some vars we need

		StringTemplate codeST = generator.getRecognizerST();
		String code = codeST.toString();
		String found = code.substring(code.indexOf("###")+3,code.indexOf("!!!"));
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testInvalidRuleLabelAccessesParameter() throws Exception {
		String action = "$r.z";
		String expecting = action;

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"a[int z] returns [int x]\n" +
				"  :\n" +
				"  ;\n"+
				"b : r=a[3] {"+action+"}\n" +
				"  ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator, "b",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		int expectedMsgID = ErrorManager.MSG_INVALID_RULE_PARAMETER_REF;
		Object expectedArg = "a";
		Object expectedArg2 = "z";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testInvalidRuleLabelAccessesScopeAttribute() throws Exception {
		String action = "$r.n";
		String expecting = action;

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"a\n" +
				"scope { int n; }\n" +
				"  :\n" +
				"  ;\n"+
				"b : r=a[3] {"+action+"}\n" +
				"  ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator, "b",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		int expectedMsgID = ErrorManager.MSG_INVALID_RULE_SCOPE_ATTRIBUTE_REF;
		Object expectedArg = "a";
		Object expectedArg2 = "n";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testInvalidRuleAttribute() throws Exception {
		String action = "$r.blort";
		String expecting = action;

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"a[int z] returns [int x]\n" +
				"  :\n" +
				"  ;\n"+
				"b : r=a[3] {"+action+"}\n" +
				"  ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator, "b",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		int expectedMsgID = ErrorManager.MSG_UNKNOWN_RULE_ATTRIBUTE;
		Object expectedArg = "a";
		Object expectedArg2 = "blort";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testMissingRuleAttribute() throws Exception {
		String action = "$r";
		String expecting = action;

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"a[int z] returns [int x]\n" +
				"  :\n" +
				"  ;\n"+
				"b : r=a[3] {"+action+"}\n" +
				"  ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator, "b",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();

		int expectedMsgID = ErrorManager.MSG_ISOLATED_RULE_SCOPE;
		Object expectedArg = "r";
		Object expectedArg2 = null;
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testMissingUnlabeledRuleAttribute() throws Exception {
		String action = "$a";
		String expecting = action;

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"a returns [int x]:\n" +
				"  ;\n"+
				"b : a {"+action+"}\n" +
				"  ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator, "b",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();

		int expectedMsgID = ErrorManager.MSG_ISOLATED_RULE_SCOPE;
		Object expectedArg = "a";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg);
		checkError(equeue, expectedMessage);
	}

	public void testNonDynamicAttributeOutsideRule() throws Exception {
		String action = "public void foo() { $x; }";
		String expecting = action;

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"@members {'+action+'}\n" +
				"a : ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,
																	 null,
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),0);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		int expectedMsgID = ErrorManager.MSG_ATTRIBUTE_REF_NOT_IN_RULE;
		Object expectedArg = "x";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg);
		checkError(equeue, expectedMessage);
	}

	public void testNonDynamicAttributeOutsideRule2() throws Exception {
		String action = "public void foo() { $x.y; }";
		String expecting = action;

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"@members {'+action+'}\n" +
				"a : ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,
																	 null,
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),0);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		int expectedMsgID = ErrorManager.MSG_ATTRIBUTE_REF_NOT_IN_RULE;
		Object expectedArg = "x";
		Object expectedArg2 = "y";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	// D Y N A M I C A L L Y  S C O P E D  A T T R I B U T E S

	public void testBasicGlobalScope() throws Exception {
		String action = "$Symbols::names.add($id.text);";
		String expecting = "((Symbols_scope)Symbols_stack.peek()).names.add(id.getText());";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"scope Symbols {\n" +
				"  int n;\n" +
				"  List names;\n" +
				"}\n" +
				"a scope Symbols; : (id=ID ';' {"+action+"} )+\n" +
				"  ;\n" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testUnknownGlobalScope() throws Exception {
		String action = "$Symbols::names.add($id.text);";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
			"a scope Symbols; : (id=ID ';' {"+action+"} )+\n" +
			"  ;\n" +
			"ID : 'a';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);

		assertEquals("unexpected errors: "+equeue, 2, equeue.errors.size());

		int expectedMsgID = ErrorManager.MSG_UNKNOWN_DYNAMIC_SCOPE;
		Object expectedArg = "Symbols";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg);
		checkError(equeue, expectedMessage);
	}

	public void testIndexedGlobalScope() throws Exception {
		String action = "$Symbols[-1]::names.add($id.text);";
		String expecting =
			"((Symbols_scope)Symbols_stack.elementAt(Symbols_stack.size()-1-1)).names.add(id.getText());";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"scope Symbols {\n" +
				"  int n;\n" +
				"  List names;\n" +
				"}\n" +
				"a scope Symbols; : (id=ID ';' {"+action+"} )+\n" +
				"  ;\n" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void test0IndexedGlobalScope() throws Exception {
		String action = "$Symbols[0]::names.add($id.text);";
		String expecting =
			"((Symbols_scope)Symbols_stack.elementAt(0)).names.add(id.getText());";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"scope Symbols {\n" +
				"  int n;\n" +
				"  List names;\n" +
				"}\n" +
				"a scope Symbols; : (id=ID ';' {"+action+"} )+\n" +
				"  ;\n" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testAbsoluteIndexedGlobalScope() throws Exception {
		String action = "$Symbols[3]::names.add($id.text);";
		String expecting =
			"((Symbols_scope)Symbols_stack.elementAt(3)).names.add(id.getText());";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"scope Symbols {\n" +
				"  int n;\n" +
				"  List names;\n" +
				"}\n" +
				"a scope Symbols; : (id=ID ';' {"+action+"} )+\n" +
				"  ;\n" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testScopeAndAttributeWithUnderscore() throws Exception {
		String action = "$foo_bar::a_b;";
		String expecting = "((foo_bar_scope)foo_bar_stack.peek()).a_b;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"scope foo_bar {\n" +
				"  int a_b;\n" +
				"}\n" +
				"a scope foo_bar; : (ID {"+action+"} )+\n" +
				"  ;\n" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();

		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testSharedGlobalScope() throws Exception {
		String action = "$Symbols::x;";
		String expecting = "((Symbols_scope)Symbols_stack.peek()).x;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"scope Symbols {\n" +
				"  String x;\n" +
				"}\n" +
				"a\n"+
				"scope { int y; }\n"+
				"scope Symbols;\n" +
				" : b {"+action+"}\n" +
				" ;\n" +
				"b : ID {$Symbols::x=$ID.text} ;\n" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testGlobalScopeOutsideRule() throws Exception {
		String action = "public void foo() {$Symbols::names.add('foo');}";
		String expecting = "public void foo() {((Symbols_scope)Symbols_stack.peek()).names.add('foo');}";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"scope Symbols {\n" +
				"  int n;\n" +
				"  List names;\n" +
				"}\n" +
				"@members {'+action+'}\n" +
				"a : \n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testRuleScopeOutsideRule() throws Exception {
		String action = "public void foo() {$a::name;}";
		String expecting = "public void foo() {((a_scope)a_stack.peek()).name;}";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"@members {"+action+"}\n" +
				"a\n" +
				"scope { int name; }\n" +
				"  : {foo();}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,
																	 null,
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),0);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testBasicRuleScope() throws Exception {
		String action = "$a::n;";
		String expecting = "((a_scope)a_stack.peek()).n;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a\n" +
				"scope {\n" +
				"  int n;\n" +
				"} : {"+action+"}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testUnqualifiedRuleScopeAccessInsideRule() throws Exception {
		String action = "$n;";
		String expecting = action;

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a\n" +
				"scope {\n" +
				"  int n;\n" +
				"} : {"+action+"}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates

		int expectedMsgID = ErrorManager.MSG_ISOLATED_RULE_ATTRIBUTE;
		Object expectedArg = "n";
		Object expectedArg2 = null;
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg,
										expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testIsolatedDynamicRuleScopeRef() throws Exception {
		String action = "$a;"; // refers to stack not top of stack
		String expecting = "a_stack;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a\n" +
				"scope {\n" +
				"  int n;\n" +
				"} : b ;\n" +
				"b : {"+action+"}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator, "b",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testDynamicRuleScopeRefInSubrule() throws Exception {
		String action = "$a::n;";
		String expecting = "((a_scope)a_stack.peek()).n;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a\n" +
				"scope {\n" +
				"  int n;\n" +
				"} : b ;\n" +
				"b : {"+action+"}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator, "b",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testIsolatedGlobalScopeRef() throws Exception {
		String action = "$Symbols;";
		String expecting = "Symbols_stack;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"scope Symbols {\n" +
				"  String x;\n" +
				"}\n" +
				"a\n"+
				"scope { int y; }\n"+
				"scope Symbols;\n" +
				" : b {"+action+"}\n" +
				" ;\n" +
				"b : ID {$Symbols::x=$ID.text} ;\n" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testRuleScopeFromAnotherRule() throws Exception {
		String action = "$a::n;"; // must be qualified
		String expecting = "((a_scope)a_stack.peek()).n;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a\n" +
				"scope {\n" +
				"  int n;\n" +
				"} : b\n" +
				"  ;\n" +
				"b : {"+action+"}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator, "b",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testFullyQualifiedRefToCurrentRuleParameter() throws Exception {
		String action = "$a.i;";
		String expecting = "i;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a[int i]: {"+action+"}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testFullyQualifiedRefToCurrentRuleRetVal() throws Exception {
		String action = "$a.i;";
		String expecting = "retval.i;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a returns [int i, int j]: {"+action+"}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testSetFullyQualifiedRefToCurrentRuleRetVal() throws Exception {
		String action = "$a.i = 1;";
		String expecting = "retval.i = 1;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
			"a returns [int i, int j]: {"+action+"}\n" +
			"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testIsolatedRefToCurrentRule() throws Exception {
		String action = "$a;";
		String expecting = "";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : 'a' {"+action+"}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates

		int expectedMsgID = ErrorManager.MSG_ISOLATED_RULE_SCOPE;
		Object expectedArg = "a";
		Object expectedArg2 = null;
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg,
										expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testIsolatedRefToRule() throws Exception {
		String action = "$x;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : x=b {"+action+"}\n" +
				"  ;\n" +
				"b : 'b' ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates

		int expectedMsgID = ErrorManager.MSG_ISOLATED_RULE_SCOPE;
		Object expectedArg = "x";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg);
		checkError(equeue, expectedMessage);
	}

	/*  I think these have to be errors $a.x makes no sense.
	public void testFullyQualifiedRefToLabelInCurrentRule() throws Exception {
			String action = "$a.x;";
			String expecting = "x;";

			ErrorQueue equeue = new ErrorQueue();
			ErrorManager.setErrorListener(equeue);
			Grammar g = new Grammar(
				"grammar t;\n"+
					"a : x='a' {"+action+"}\n" +
					"  ;\n");
			Tool antlr = newTool();
			CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
			g.setCodeGenerator(generator);
			generator.genRecognizer(); // forces load of templates
			ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
															   new antlr.CommonToken(ANTLRParser.ACTION,action),1);
			String rawTranslation =
				translator.translate();
			StringTemplateGroup templates =
				new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
			StringTemplate actionST = new StringTemplate(templates, rawTranslation);
			String found = actionST.toString();
			assertEquals(expecting, found);

			assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
		}

	public void testFullyQualifiedRefToListLabelInCurrentRule() throws Exception {
		String action = "$a.x;"; // must be qualified
		String expecting = "list_x;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : x+='a' {"+action+"}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
														   new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}
*/
	public void testFullyQualifiedRefToTemplateAttributeInCurrentRule() throws Exception {
		String action = "$a.st;"; // can be qualified
		String expecting = "retval.st;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n" +
				"options {output=template;}\n"+
				"a : (A->{$A.text}) {"+action+"}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testRuleRefWhenRuleHasScope() throws Exception {
		String action = "$b.start;";
		String expecting = "((Token)b1.start);";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n" +
				"a : b {###"+action+"!!!} ;\n" +
				"b\n" +
				"scope {\n" +
				"  int n;\n" +
				"} : 'b' \n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates

		StringTemplate codeST = generator.getRecognizerST();
		String code = codeST.toString();
		String found = code.substring(code.indexOf("###")+3,code.indexOf("!!!"));
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testDynamicScopeRefOkEvenThoughRuleRefExists() throws Exception {
		String action = "$b::n;";
		String expecting = "((b_scope)b_stack.peek()).n;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n" +
				"s : b ;\n"+
				"b\n" +
				"scope {\n" +
				"  int n;\n" +
				"} : '(' b ')' {"+action+"}\n" + // refers to current invocation's n
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator, "b",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testRefToTemplateAttributeForCurrentRule() throws Exception {
		String action = "$st=null;";
		String expecting = "retval.st =null;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n" +
				"options {output=template;}\n"+
				"a : {"+action+"}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testRefToTextAttributeForCurrentRule() throws Exception {
		String action = "$text";
		String expecting = "input.toString(retval.start,input.LT(-1))";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n" +
				"options {output=template;}\n"+
				"a : {"+action+"}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testRefToStartAttributeForCurrentRule() throws Exception {
		String action = "$start;";
		String expecting = "((Token)retval.start);";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n" +
				"a : {###"+action+"!!!}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		StringTemplate codeST = generator.getRecognizerST();
		String code = codeST.toString();
		String found = code.substring(code.indexOf("###")+3,code.indexOf("!!!"));
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testTokenLabelFromMultipleAlts() throws Exception {
		String action = "$ID.text;"; // must be qualified
		String action2 = "$INT.text;"; // must be qualified
		String expecting = "ID1.getText();";
		String expecting2 = "INT2.getText();";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : ID {"+action+"}\n" +
				"  | INT {"+action2+"}\n" +
				"  ;\n" +
				"ID : 'a';\n" +
				"INT : '0';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
		translator = new ActionTranslatorLexer(generator,
											   "a",
											   new antlr.CommonToken(ANTLRParser.ACTION,action2),2);
		rawTranslation =
			translator.translate();
		templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		actionST = new StringTemplate(templates, rawTranslation);
		found = actionST.toString();

		assertEquals(expecting2, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testRuleLabelFromMultipleAlts() throws Exception {
		String action = "$b.text;"; // must be qualified
		String action2 = "$c.text;"; // must be qualified
		String expecting = "input.toString(b1.start,b1.stop);";
		String expecting2 = "input.toString(c2.start,c2.stop);";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : b {"+action+"}\n" +
				"  | c {"+action2+"}\n" +
				"  ;\n" +
				"b : 'a';\n" +
				"c : '0';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
		translator = new ActionTranslatorLexer(generator,
											   "a",
											   new antlr.CommonToken(ANTLRParser.ACTION,action2),2);
		rawTranslation =
			translator.translate();
		templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		actionST = new StringTemplate(templates, rawTranslation);
		found = actionST.toString();

		assertEquals(expecting2, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testUnknownDynamicAttribute() throws Exception {
		String action = "$a::x";
		String expecting = action;

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a\n" +
				"scope {\n" +
				"  int n;\n" +
				"} : {"+action+"}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "a",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		int expectedMsgID = ErrorManager.MSG_UNKNOWN_DYNAMIC_SCOPE_ATTRIBUTE;
		Object expectedArg = "a";
		Object expectedArg2 = "x";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testUnknownGlobalDynamicAttribute() throws Exception {
		String action = "$Symbols::x";
		String expecting = action;

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"scope Symbols {\n" +
				"  int n;\n" +
				"}\n" +
				"a : {'+action+'}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "a",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		int expectedMsgID = ErrorManager.MSG_UNKNOWN_DYNAMIC_SCOPE_ATTRIBUTE;
		Object expectedArg = "Symbols";
		Object expectedArg2 = "x";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testUnqualifiedRuleScopeAttribute() throws Exception {
		String action = "$n;"; // must be qualified
		String expecting = "$n;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a\n" +
				"scope {\n" +
				"  int n;\n" +
				"} : b\n" +
				"  ;\n" +
				"b : {'+action+'}\n" +
				"  ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "b",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		int expectedMsgID = ErrorManager.MSG_UNKNOWN_SIMPLE_ATTRIBUTE;
		Object expectedArg = "n";
		Object expectedArg2 = null;
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testRuleAndTokenLabelTypeMismatch() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : id='foo' id=b\n" +
				"  ;\n" +
				"b : ;\n");
		int expectedMsgID = ErrorManager.MSG_LABEL_TYPE_CONFLICT;
		Object expectedArg = "id";
		Object expectedArg2 = "rule!=token";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testListAndTokenLabelTypeMismatch() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : ids+='a' ids='b'\n" +
				"  ;\n" +
				"b : ;\n");
		int expectedMsgID = ErrorManager.MSG_LABEL_TYPE_CONFLICT;
		Object expectedArg = "ids";
		Object expectedArg2 = "token!=token-list";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testListAndRuleLabelTypeMismatch() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n" +
				"options {output=AST;}\n"+
				"a : bs+=b bs=b\n" +
				"  ;\n" +
				"b : 'b';\n");
		int expectedMsgID = ErrorManager.MSG_LABEL_TYPE_CONFLICT;
		Object expectedArg = "bs";
		Object expectedArg2 = "rule!=rule-list";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testArgReturnValueMismatch() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a[int i] returns [int x, int i]\n" +
				"  : \n" +
				"  ;\n" +
				"b : ;\n");
		int expectedMsgID = ErrorManager.MSG_ARG_RETVAL_CONFLICT;
		Object expectedArg = "i";
		Object expectedArg2 = "a";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testSimplePlusEqualLabel() throws Exception {
		String action = "$ids.size();"; // must be qualified
		String expecting = "list_ids.size();";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"parser grammar t;\n"+
				"a : ids+=ID ( COMMA ids+=ID {"+action+"})* ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "a",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testPlusEqualStringLabel() throws Exception {
		String action = "$ids.size();"; // must be qualified
		String expecting = "list_ids.size();";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : ids+='if' ( ',' ids+=ID {"+action+"})* ;" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "a",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testPlusEqualSetLabel() throws Exception {
		String action = "$ids.size();"; // must be qualified
		String expecting = "list_ids.size();";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : ids+=('a'|'b') ( ',' ids+=ID {"+action+"})* ;" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "a",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testPlusEqualWildcardLabel() throws Exception {
		String action = "$ids.size();"; // must be qualified
		String expecting = "list_ids.size();";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : ids+=. ( ',' ids+=ID {"+action+"})* ;" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "a",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testImplicitTokenLabel() throws Exception {
		String action = "$ID; $ID.text; $ID.getText()";
		String expecting = "ID1; ID1.getText(); ID1.getText()";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : ID {"+action+"} ;" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");

		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "a",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testImplicitRuleLabel() throws Exception {
		String action = "$r.start;";
		String expecting = "((Token)r1.start);";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : r {###"+action+"!!!} ;" +
				"r : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();

		StringTemplate codeST = generator.getRecognizerST();
		String code = codeST.toString();
		String found = code.substring(code.indexOf("###")+3,code.indexOf("!!!"));
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testReuseExistingLabelWithImplicitRuleLabel() throws Exception {
		String action = "$r.start;";
		String expecting = "((Token)x.start);";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : x=r {###"+action+"!!!} ;" +
				"r : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();

		StringTemplate codeST = generator.getRecognizerST();
		String code = codeST.toString();
		String found = code.substring(code.indexOf("###")+3,code.indexOf("!!!"));
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testReuseExistingListLabelWithImplicitRuleLabel() throws Exception {
		String action = "$r.start;";
		String expecting = "((Token)x.start);";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"options {output=AST;}\n" +
				"a : x+=r {###"+action+"!!!} ;" +
				"r : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();

		StringTemplate codeST = generator.getRecognizerST();
		String code = codeST.toString();
		String found = code.substring(code.indexOf("###")+3,code.indexOf("!!!"));
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testReuseExistingLabelWithImplicitTokenLabel() throws Exception {
		String action = "$ID.text;";
		String expecting = "x.getText();";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : x=ID {"+action+"} ;" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();

		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testReuseExistingListLabelWithImplicitTokenLabel() throws Exception {
		String action = "$ID.text;";
		String expecting = "x.getText();";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : x+=ID {"+action+"} ;" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();

		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testMissingArgs() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : r ;" +
				"r[int i] : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();

		int expectedMsgID = ErrorManager.MSG_MISSING_RULE_ARGS;
		Object expectedArg = "r";
		Object expectedArg2 = null;
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testArgsWhenNoneDefined() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : r[32,34] ;" +
				"r : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();

		int expectedMsgID = ErrorManager.MSG_RULE_HAS_NO_ARGS;
		Object expectedArg = "r";
		Object expectedArg2 = null;
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testReturnInitValue() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
			"a : r ;\n" +
			"r returns [int x=0] : 'a' {$x = 4;} ;\n");
		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());

		Rule r = g.getRule("r");
		AttributeScope retScope = r.returnScope;
		List parameters = retScope.getAttributes();
		assertNotNull("missing return action", parameters);
		assertEquals(1, parameters.size());
		String found = parameters.get(0).toString();
		String expecting = "int x=0";
		assertEquals(expecting, found);
	}

	public void testMultipleReturnInitValue() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
			"a : r ;\n" +
			"r returns [int x=0, int y, String s=new String(\"foo\")] : 'a' {$x = 4;} ;\n");
		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());

		Rule r = g.getRule("r");
		AttributeScope retScope = r.returnScope;
		List parameters = retScope.getAttributes();
		assertNotNull("missing return action", parameters);
		assertEquals(3, parameters.size());
		assertEquals("int x=0", parameters.get(0).toString());
		assertEquals("int y", parameters.get(1).toString());
		assertEquals("String s=new String(\"foo\")", parameters.get(2).toString());
	}

	public void testCStyleReturnInitValue() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
			"a : r ;\n" +
			"r returns [int (*x)()=NULL] : 'a' ;\n");
		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());

		Rule r = g.getRule("r");
		AttributeScope retScope = r.returnScope;
		List parameters = retScope.getAttributes();
		assertNotNull("missing return action", parameters);
		assertEquals(1, parameters.size());
		String found = parameters.get(0).toString();
		String expecting = "int (*)() x=NULL";
		assertEquals(expecting, found);
	}

	public void testArgsWithInitValues() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : r[32,34] ;" +
				"r[int x, int y=3] : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();

		int expectedMsgID = ErrorManager.MSG_ARG_INIT_VALUES_ILLEGAL;
		Object expectedArg = "y";
		Object expectedArg2 = null;
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testArgsOnToken() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : ID[32,34] ;" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();

		int expectedMsgID = ErrorManager.MSG_ARGS_ON_TOKEN_REF;
		Object expectedArg = "ID";
		Object expectedArg2 = null;
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testArgsOnTokenInLexer() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"lexer grammar t;\n"+
				"R : 'z' ID[32,34] ;" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();

		int expectedMsgID = ErrorManager.MSG_RULE_HAS_NO_ARGS;
		Object expectedArg = "ID";
		Object expectedArg2 = null;
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testLabelOnRuleRefInLexer() throws Exception {
		String action = "$i.text";
		String expecting = "i.getText()";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"lexer grammar t;\n"+
				"R : 'z' i=ID {"+action+"};" +
				"fragment ID : 'a';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "R",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();

		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testRefToRuleRefInLexer() throws Exception {
		String action = "$ID.text";
		String expecting = "ID1.getText()";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"lexer grammar t;\n"+
				"R : 'z' ID {"+action+"};" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "R",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();

		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testRefToRuleRefInLexerNoAttribute() throws Exception {
		String action = "$ID";
		String expecting = "ID1";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"lexer grammar t;\n"+
				"R : 'z' ID {"+action+"};" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "R",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();

		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testCharLabelInLexer() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"lexer grammar t;\n"+
				"R : x='z' ;\n");

		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testCharListLabelInLexer() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"lexer grammar t;\n"+
				"R : x+='z' ;\n");

		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testWildcardCharLabelInLexer() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"lexer grammar t;\n"+
				"R : x=. ;\n");

		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testWildcardCharListLabelInLexer() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"lexer grammar t;\n"+
				"R : x+=. ;\n");

		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testMissingArgsInLexer() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"lexer grammar t;\n"+
				"A : R ;" +
				"R[int i] : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();

		int expectedMsgID = ErrorManager.MSG_MISSING_RULE_ARGS;
		Object expectedArg = "R";
		Object expectedArg2 = null;
		// getting a second error @1:12, probably from nextToken
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testLexerRulePropertyRefs() throws Exception {
		String action = "$text $type $line $pos $channel $index";
		String expecting = "getText() _type _line _charPosition _channel -1";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"lexer grammar t;\n"+
				"R : 'r' {"+action+"};\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "R",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();

		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testLexerLabelRefs() throws Exception {
		String action = "$a $b.text $c $d.text";
		String expecting = "a b.getText() c d.getText()";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"lexer grammar t;\n"+
				"R : a='c' b='hi' c=. d=DUH {"+action+"};\n" +
				"DUH : 'd' ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "R",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();

		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testSettingLexerRulePropertyRefs() throws Exception {
		String action = "$text $type=1 $line=1 $pos=1 $channel=1 $index";
		String expecting = "getText() _type=1 _line=1 _charPosition=1 _channel=1 -1";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"lexer grammar t;\n"+
				"R : 'r' {"+action+"};\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "R",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();

		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testArgsOnTokenInLexerRuleOfCombined() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : R;\n" +
				"R : 'z' ID[32] ;\n" +
				"ID : 'a';\n");

		String lexerGrammarStr = g.getLexerGrammar();
		StringReader sr = new StringReader(lexerGrammarStr);
		Grammar lexerGrammar = new Grammar();
		lexerGrammar.setFileName("<internally-generated-lexer>");
		lexerGrammar.importTokenVocabulary(g);
		lexerGrammar.setGrammarContent(sr);
		sr.close();

		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, lexerGrammar, "Java");
		lexerGrammar.setCodeGenerator(generator);
		generator.genRecognizer();

		int expectedMsgID = ErrorManager.MSG_RULE_HAS_NO_ARGS;
		Object expectedArg = "ID";
		Object expectedArg2 = null;
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, lexerGrammar, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testMissingArgsOnTokenInLexerRuleOfCombined() throws Exception {
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : R;\n" +
				"R : 'z' ID ;\n" +
				"ID[int i] : 'a';\n");

		String lexerGrammarStr = g.getLexerGrammar();
		StringReader sr = new StringReader(lexerGrammarStr);
		Grammar lexerGrammar = new Grammar();
		lexerGrammar.setFileName("<internally-generated-lexer>");
		lexerGrammar.importTokenVocabulary(g);
		lexerGrammar.setGrammarContent(sr);
		sr.close();

		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, lexerGrammar, "Java");
		lexerGrammar.setCodeGenerator(generator);
		generator.genRecognizer();

		int expectedMsgID = ErrorManager.MSG_MISSING_RULE_ARGS;
		Object expectedArg = "ID";
		Object expectedArg2 = null;
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, lexerGrammar, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	// T R E E S

	public void testTokenLabelTreeProperty() throws Exception {
		String action = "$id.tree;";
		String expecting = "id_tree;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : id=ID {"+action+"} ;\n" +
				"ID : 'a';\n");

		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		ActionTranslatorLexer translator =
			new ActionTranslatorLexer(generator,
									  "a",
									  new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testTokenRefTreeProperty() throws Exception {
		String action = "$ID.tree;";
		String expecting = "ID1_tree;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : ID {"+action+"} ;" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();

		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,"a",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);
	}

	public void testAmbiguousTokenRef() throws Exception {
		String action = "$ID;";
		String expecting = "";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : ID ID {"+action+"};" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();

		int expectedMsgID = ErrorManager.MSG_NONUNIQUE_REF;
		Object expectedArg = "ID";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg);
		checkError(equeue, expectedMessage);
	}

	public void testAmbiguousTokenRefWithProp() throws Exception {
		String action = "$ID.text;";
		String expecting = "";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar t;\n"+
				"a : ID ID {"+action+"};" +
				"ID : 'a';\n");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();

		int expectedMsgID = ErrorManager.MSG_NONUNIQUE_REF;
		Object expectedArg = "ID";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg);
		checkError(equeue, expectedMessage);
	}

	public void testRuleRefWithDynamicScope() throws Exception {
		String action = "$field::x = $field.st;";
		String expecting = "((field_scope)field_stack.peek()).x = retval.st;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar a;\n" +
				"field\n" +
				"scope { StringTemplate x; }\n" +
				"    :   'y' {"+action+"}\n" +
				"    ;\n");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,
																	 "field",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testAssignToOwnRulenameAttr() throws Exception {
		String action = "$rule.tree = null;";
		String expecting = "retval.tree = null;";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar a;\n" +
			"rule\n" +
			"    : 'y' {" + action +"}\n" +
			"    ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,
																	 "rule",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testAssignToOwnParamAttr() throws Exception {
		String action = "$rule.i = 42; $i = 23;";
		String expecting = "i = 42; i = 23;";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar a;\n" +
			"rule[int i]\n" +
			"    : 'y' {" + action +"}\n" +
			"    ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,
																	 "rule",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
	}

	public void testIllegalAssignToOwnRulenameAttr() throws Exception {
		String action = "$rule.stop = 0;";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar a;\n" +
			"rule\n" +
			"    : 'y' {" + action +"}\n" +
			"    ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,
																	 "rule",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();

		int expectedMsgID = ErrorManager.MSG_WRITE_TO_READONLY_ATTR;
		Object expectedArg = "rule";
		Object expectedArg2 = "stop";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testIllegalAssignToLocalAttr() throws Exception {
		String action = "$tree = null; $st = null; $start = 0; $stop = 0; $text = 0;";
		String expecting = "retval.tree = null; retval.st = null;   ";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar a;\n" +
			"rule\n" +
			"    : 'y' {" + action +"}\n" +
			"    ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,
																	 "rule",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();

		int expectedMsgID = ErrorManager.MSG_WRITE_TO_READONLY_ATTR;
		ArrayList expectedErrors = new ArrayList(3);
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, "start", "");
		expectedErrors.add(expectedMessage);
		GrammarSemanticsMessage expectedMessage2 =
			new GrammarSemanticsMessage(expectedMsgID, g, null, "stop", "");
		expectedErrors.add(expectedMessage2);
				GrammarSemanticsMessage expectedMessage3 =
			new GrammarSemanticsMessage(expectedMsgID, g, null, "text", "");
		expectedErrors.add(expectedMessage3);
		checkErrors(equeue, expectedErrors);

		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals(expecting, found);
	}

	public void testIllegalAssignRuleRefAttr() throws Exception {
		String action = "$other.tree = null;";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar a;\n" +
			"options { output = AST;}" +
			"otherrule\n" +
			"    : 'y' ;" +
			"rule\n" +
			"    : other=otherrule {" + action +"}\n" +
			"    ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,
																	 "rule",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();

		int expectedMsgID = ErrorManager.MSG_WRITE_TO_READONLY_ATTR;
		Object expectedArg = "other";
		Object expectedArg2 = "tree";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testIllegalAssignTokenRefAttr() throws Exception {
		String action = "$ID.text = \"test\";";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar a;\n" +
			"ID\n" +
			"    : 'y' ;" +
			"rule\n" +
			"    : ID {" + action +"}\n" +
			"    ;");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,
																	 "rule",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();

		int expectedMsgID = ErrorManager.MSG_WRITE_TO_READONLY_ATTR;
		Object expectedArg = "ID";
		Object expectedArg2 = "text";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		checkError(equeue, expectedMessage);
	}

	public void testAssignToTreeNodeAttribute() throws Exception {
		String action = "$tree.scope = localScope;";
		String expecting = "(()retval.tree).scope = localScope;";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"grammar a;\n" +
			"options { output=AST; }" +
			"rule\n" +
			"@init {\n" +
			"   Scope localScope=null;\n" +
			"}\n" +
			"@finally {\n" +
			"   $tree.scope = localScope;\n" +
			"}\n" +
			"   : 'a' -> ^('a')\n" +
			";");
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // forces load of templates
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,
																	 "rule",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
		assertEquals(expecting, found);
	}

	public void testDoNotTranslateAttributeCompare() throws Exception {
		String action = "$a.line == $b.line";
		String expecting = "a.getLine() == b.getLine()";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
				"lexer grammar a;\n" +
				"RULE:\n" +
				"     a=ID b=ID {" + action + "}" +
				"    ;\n" +
				"ID : 'id';"
		);
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,
																	 "RULE",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
		assertEquals(expecting, found);
	}

	public void testDoNotTranslateScopeAttributeCompare() throws Exception {
		String action = "if ($rule::foo == \"foo\" || 1) { System.out.println(\"ouch\"); }";
		String expecting = "if (((rule_scope)rule_stack.peek()).foo == \"foo\" || 1) { System.out.println(\"ouch\"); }";
		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
				"grammar a;\n" +
				"rule\n" +
				"scope {\n" +
				"   String foo;" +
				"} :\n" +
				"     twoIDs" +
				"    ;\n" +
				"twoIDs:\n" +
				"    ID ID {" + action + "}\n" +
				"    ;\n" +
				"ID : 'id';"
		);
		Tool antlr = newTool();
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer();
		ActionTranslatorLexer translator = new ActionTranslatorLexer(generator,
																	 "twoIDs",
																	 new antlr.CommonToken(ANTLRParser.ACTION,action),1);
		String rawTranslation =
			translator.translate();
		// check that we didn't use scopeSetAttributeRef int translation!
		boolean foundScopeSetAttributeRef = false;
		for (int i = 0; i < translator.chunks.size(); i++) {
			Object chunk = translator.chunks.get(i);
			if (chunk instanceof StringTemplate) {
				if (((StringTemplate)chunk).getName().equals("scopeSetAttributeRef")) {
					foundScopeSetAttributeRef = true;
				}
			}
		}
		assertFalse("action translator used scopeSetAttributeRef template in comparison!", foundScopeSetAttributeRef);
		StringTemplateGroup templates =
			new StringTemplateGroup(".", AngleBracketTemplateLexer.class);
		StringTemplate actionST = new StringTemplate(templates, rawTranslation);
		String found = actionST.toString();
		assertEquals("unexpected errors: "+equeue, 0, equeue.errors.size());
		assertEquals(expecting, found);
	}

	public void testTreeRuleStopAttributeIsInvalid() throws Exception {
		String action = "$r.x; $r.start; $r.stop;";
		String expecting = "r.x; ((Object)r.start); $r.stop;";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"tree grammar t;\n"+
				"a returns [int x]\n" +
				"  :\n" +
				"  ;\n"+
				"b : r=a {###"+action+"!!!}\n" +
				"  ;");
		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // codegen phase sets some vars we need
		StringTemplate codeST = generator.getRecognizerST();
		String code = codeST.toString();
		String found = code.substring(code.indexOf("###")+3,code.indexOf("!!!"));
		assertEquals(expecting, found);

		int expectedMsgID = ErrorManager.MSG_UNKNOWN_RULE_ATTRIBUTE;
		Object expectedArg = "a";
		Object expectedArg2 = "stop";
		GrammarSemanticsMessage expectedMessage =
			new GrammarSemanticsMessage(expectedMsgID, g, null, expectedArg, expectedArg2);
		System.out.println("equeue:"+equeue);
		checkError(equeue, expectedMessage);
	}

	public void testRefToTextAttributeForCurrentTreeRule() throws Exception {
		String action = "$text";
		String expecting = "input.getTokenStream().toString(\n" +
			"              input.getTreeAdaptor().getTokenStartIndex(retval.start),\n" +
			"              input.getTreeAdaptor().getTokenStopIndex(retval.start))";

		ErrorQueue equeue = new ErrorQueue();
		ErrorManager.setErrorListener(equeue);
		Grammar g = new Grammar(
			"tree grammar t;\n" +
			"a : {###"+action+"!!!}\n" +
			"  ;\n");

		Tool antlr = newTool();
		antlr.setOutputDirectory(null); // write to /dev/null
		CodeGenerator generator = new CodeGenerator(antlr, g, "Java");
		g.setCodeGenerator(generator);
		generator.genRecognizer(); // codegen phase sets some vars we need
		StringTemplate codeST = generator.getRecognizerST();
		String code = codeST.toString();
		String found = code.substring(code.indexOf("###")+3,code.indexOf("!!!"));
		assertEquals(expecting, found);

		assertEquals("unexpected errors: "+equeue, 1, equeue.errors.size());
	}


	// S U P P O R T

	protected void checkError(ErrorQueue equeue,
							  GrammarSemanticsMessage expectedMessage)
		throws Exception
	{
		/*
		System.out.println(equeue.infos);
		System.out.println(equeue.warnings);
		System.out.println(equeue.errors);
		*/
		Message foundMsg = null;
		for (int i = 0; i < equeue.errors.size(); i++) {
			Message m = (Message)equeue.errors.get(i);
			if (m.msgID==expectedMessage.msgID ) {
				foundMsg = m;
			}
		}
		assertTrue("no error; "+expectedMessage.msgID+" expected", equeue.errors.size() > 0);
		assertNotNull("couldn't find expected error: "+expectedMessage.msgID, foundMsg);
		assertTrue("error is not a GrammarSemanticsMessage",
				   foundMsg instanceof GrammarSemanticsMessage);
		assertEquals(expectedMessage.arg, foundMsg.arg);
		assertEquals(expectedMessage.arg2, foundMsg.arg2);
	}

	/** Allow checking for multiple errors in one test */
	protected void checkErrors(ErrorQueue equeue,
							   ArrayList expectedMessages)
			throws Exception
	{
		ArrayList messageExpected = new ArrayList(equeue.errors.size());
		for (int i = 0; i < equeue.errors.size(); i++) {
			Message m = (Message)equeue.errors.get(i);
			boolean foundMsg = false;
			for (int j = 0; j < expectedMessages.size(); j++) {
				Message em = (Message)expectedMessages.get(j);
				if (m.msgID==em.msgID && m.arg.equals(em.arg) && m.arg2.equals(em.arg2)) {
					foundMsg = true;
				}
			}
			if (foundMsg) {
				messageExpected.add(i, Boolean.TRUE);
			} else
				messageExpected.add(i, Boolean.FALSE);
		}
		for (int i = 0; i < equeue.errors.size(); i++) {
			assertTrue("unexpected error:" + equeue.errors.get(i), ((Boolean)messageExpected.get(i)).booleanValue());
		}
	}
}
