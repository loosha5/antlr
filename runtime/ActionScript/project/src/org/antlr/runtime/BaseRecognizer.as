package org.antlr.runtime {
    import flash.events.ErrorEvent;
    import flash.events.EventDispatcher;
    
	
	
	/** A generic recognizer that can handle recognizers generated from
	 *  lexer, parser, and tree grammars.  This is all the parsing
	 *  support code essentially; most of it is error recovery stuff and
	 *  backtracking.
	 */
	public class BaseRecognizer extends EventDispatcher {
		public static const MEMO_RULE_FAILED:int = -2;
		public static const MEMO_RULE_UNKNOWN:int = -1;
		public static const INITIAL_FOLLOW_STACK_SIZE:int = 100;
	
		// GMS: need to see where this is used.
		//public static final Integer MEMO_RULE_FAILED_I = new Integer(MEMO_RULE_FAILED);
	
		// copies from Token object for convenience in actions
		public static const DEFAULT_TOKEN_CHANNEL:int = TokenConstants.DEFAULT_CHANNEL;
		public static const HIDDEN:int = TokenConstants.HIDDEN_CHANNEL;
	
		public static const NEXT_TOKEN_RULE_NAME:String = "nextToken";
	
		/** State of a lexer, parser, or tree parser are collected into a state
		 *  object so the state can be shared.  This sharing is needed to
		 *  have one grammar import others and share same error variables
		 *  and other state variables.  It's a kind of explicit multiple
		 *  inheritance via delegation of methods and shared state.
		 * 
		 */
		public var state:RecognizerSharedState;  // Note - this is public due ActionScript access rules -- GMS
	
		public function BaseRecognizer(state:RecognizerSharedState = null) {
			if ( state!=null ) { // don't ever let us have a null state
				this.state = state;
			}
			else {
				this.state = new RecognizerSharedState();			
			}
		}
	
		/** reset the parser's state; subclasses must rewinds the input stream */
		public function reset():void {
			// wack everything related to error recovery
			state._fsp = -1;
			state.errorRecovery = false;
			state.lastErrorIndex = -1;
			state.failed = false;
			// wack everything related to backtracking and memoization
			state.backtracking = 0;
			for (var i:int = 0; state.ruleMemo!=null && i < state.ruleMemo.length; i++) { // wipe cache
				state.ruleMemo[i] = null;
			}
		}
	
		/** Match current input symbol against ttype.  Upon error, do one token
		 *  insertion or deletion if possible.  You can override to not recover
		 *  here and bail out of the current production to the normal error
		 *  exception catch (at the end of the method) by just throwing
		 *  MismatchedTokenException upon input.LA(1)!=ttype.
		 * 
		 * GMS - renamed matchStream from match
		 */
		public function matchStream(input:IntStream, ttype:int, follow:BitSet):void
		{
			if ( input.LA(1)==ttype ) {
				input.consume();
				state.errorRecovery = false;
				state.failed = false;
				return;
			}
			if ( state.backtracking>0 ) {
				state.failed = true;
				return;
			}
			mismatch(input, ttype, follow);
			return;
		}
	
		// GMS - renamed from matchAny to matchAnyStream
		public function matchAnyStream(input:IntStream):void {
			state.errorRecovery = false;
			state.failed = false;
			input.consume();
		}
	
		/** factor out what to do upon token mismatch so tree parsers can behave
		 *  differently.  Override this method in your parser to do things
		 *  like bailing out after the first error; just throw the mte object
		 *  instead of calling the recovery method.
		 */
		protected function mismatch(input:IntStream, ttype:int, follow:BitSet):void
		{
			var mte:MismatchedTokenException =
				new MismatchedTokenException(ttype, input);
			recoverFromMismatchedToken(input, mte, ttype, follow);
		}
	
		/** Report a recognition problem.
		 *
		 *  This method sets errorRecovery to indicate the parser is recovering
		 *  not parsing.  Once in recovery mode, no errors are generated.
		 *  To get out of recovery mode, the parser must successfully match
		 *  a token (after a resync).  So it will go:
		 *
		 * 		1. error occurs
		 * 		2. enter recovery mode, report error
		 * 		3. consume until token found in resynch set
		 * 		4. try to resume parsing
		 * 		5. next match() will reset errorRecovery mode
		 */
		public function reportError(e:RecognitionException):void {
			// if we've already reported an error and have not matched a token
			// yet successfully, don't report any errors.
			if ( state.errorRecovery ) {
				//System.err.print("[SPURIOUS] ");
				return;
			}
			state.errorRecovery = true;
	
			displayRecognitionError(this.tokenNames, e);
		}
	
		public function displayRecognitionError(tokenNames:Array,
											e:RecognitionException):void
		{
			var hdr:String = getErrorHeader(e);
			var msg:String = getErrorMessage(e, tokenNames);
			emitErrorMessage(hdr+" "+msg);
		}
	
		/** What error message should be generated for the various
		 *  exception types?
		 *
		 *  Not very object-oriented code, but I like having all error message
		 *  generation within one method rather than spread among all of the
		 *  exception classes. This also makes it much easier for the exception
		 *  handling because the exception classes do not have to have pointers back
		 *  to this object to access utility routines and so on. Also, changing
		 *  the message for an exception type would be difficult because you
		 *  would have to subclassing exception, but then somehow get ANTLR
		 *  to make those kinds of exception objects instead of the default.
		 *  This looks weird, but trust me--it makes the most sense in terms
		 *  of flexibility.
		 *
		 *  For grammar debugging, you will want to override this to add
		 *  more information such as the stack frame with
		 *  getRuleInvocationStack(e, this.getClass().getName()) and,
		 *  for no viable alts, the decision description and state etc...
		 *
		 *  Override this to change the message generated for one or more
		 *  exception types.
		 */
		public function getErrorMessage(e:RecognitionException, tokenNames:Array):String {
			var msg:String = null;
			var tokenName:String = null;
			if ( e is MismatchedTokenException ) {
				var mte:MismatchedTokenException = MismatchedTokenException(e);
				tokenName="<unknown>";
				if ( mte.expecting== TokenConstants.EOF ) {
					tokenName = "EOF";
				}
				else {
					tokenName = tokenNames[mte.expecting];
				}
				msg = "mismatched input "+getTokenErrorDisplay(e.token)+
					" expecting "+tokenName;
			}
			else if ( e is MismatchedTreeNodeException ) {
				var mtne:MismatchedTreeNodeException = MismatchedTreeNodeException(e);
				tokenName="<unknown>";
				if ( mtne.expecting==TokenConstants.EOF ) {
					tokenName = "EOF";
				}
				else {
					tokenName = tokenNames[mtne.expecting];
				}
				msg = "mismatched tree node: "+mtne.node+
					" expecting "+tokenName;
			}
			else if ( e is NoViableAltException ) {
				var nvae:NoViableAltException = NoViableAltException(e);
				// for development, can add "decision=<<"+nvae.grammarDecisionDescription+">>"
				// and "(decision="+nvae.decisionNumber+") and
				// "state "+nvae.stateNumber
				msg = "no viable alternative at input "+getTokenErrorDisplay(e.token);
			}
			else if ( e is EarlyExitException ) {
				var eee:EarlyExitException = EarlyExitException(e);
				// for development, can add "(decision="+eee.decisionNumber+")"
				msg = "required (...)+ loop did not match anything at input "+
					getTokenErrorDisplay(e.token);
			}
			else if ( e is MismatchedSetException ) {
				var mse:MismatchedSetException = MismatchedSetException(e);
				msg = "mismatched input "+getTokenErrorDisplay(e.token)+
					" expecting set "+mse.expecting;
			}
			else if ( e is MismatchedNotSetException ) {
				var mnse:MismatchedNotSetException = MismatchedNotSetException(e);
				msg = "mismatched input "+getTokenErrorDisplay(e.token)+
					" expecting set "+mnse.expecting;
			}
			else if ( e is FailedPredicateException ) {
				var fpe:FailedPredicateException = FailedPredicateException(e);
				msg = "rule "+fpe.ruleName+" failed predicate: {"+
					fpe.predicateText+"}?";
			}
			return msg;
		}
	
		/** What is the error header, normally line/character position information? */
		public function getErrorHeader(e:RecognitionException):String {
			return "line "+e.line+":"+e.charPositionInLine;
		}
	
		/** How should a token be displayed in an error message? The default
		 *  is to display just the text, but during development you might
		 *  want to have a lot of information spit out.  Override in that case
		 *  to use t.toString() (which, for CommonToken, dumps everything about
		 *  the token). This is better than forcing you to override a method in
		 *  your token objects because you don't have to go modify your lexer
		 *  so that it creates a new Java type.
		 */
		public function getTokenErrorDisplay(t:Token):String {
			var s:String = t.text;
			if ( s==null ) {
				if ( t.type==TokenConstants.EOF ) {
					s = "<EOF>";
				}
				else {
					s = "<"+t.type+">";
				}
			}
			// GMS TODO - fix this
			//s = s.replaceAll("\n","\\\\n");
			//s = s.replaceAll("\r","\\\\r");
			//s = s.replaceAll("\t","\\\\t");
			return "'"+s+"'";
		}
	
		/** Override this method to change where error messages go */
		public function emitErrorMessage(msg:String):void {
			if (hasEventListener(ErrorEvent.ERROR)) {
			    dispatchEvent(new ErrorEvent(ErrorEvent.ERROR, false, false, msg));
			}
			else {
			    trace(msg);
			}
		}
	
		/** Recover from an error found on the input stream.  Mostly this is
		 *  NoViableAlt exceptions, but could be a mismatched token that
		 *  the match() routine could not recover from.
		 * 
		 * GMS - renamed to recoverStream from recover()
		 */
		public function recoverStream(input:IntStream, re:RecognitionException):void {
			if ( state.lastErrorIndex==input.index) {
				// uh oh, another error at same token index; must be a case
				// where LT(1) is in the recovery token set so nothing is
				// consumed; consume a single token so at least to prevent
				// an infinite loop; this is a failsafe.
				input.consume();
			}
			state.lastErrorIndex = input.index;
			var followSet:BitSet = computeErrorRecoverySet();
			beginResync();
			consumeUntil(input, followSet);
			endResync();
		}
	
		/** A hook to listen in on the token consumption during error recovery.
		 *  The DebugParser subclasses this to fire events to the listenter.
		 */
		public function beginResync():void {
		}
	
		public function endResync():void {
		}
	
		/*  Compute the error recovery set for the current rule.  During
		 *  rule invocation, the parser pushes the set of tokens that can
		 *  follow that rule reference on the stack; this amounts to
		 *  computing FIRST of what follows the rule reference in the
		 *  enclosing rule. This local follow set only includes tokens
		 *  from within the rule; i.e., the FIRST computation done by
		 *  ANTLR stops at the end of a rule.
		 *
		 *  EXAMPLE
		 *
		 *  When you find a "no viable alt exception", the input is not
		 *  consistent with any of the alternatives for rule r.  The best
		 *  thing to do is to consume tokens until you see something that
		 *  can legally follow a call to r *or* any rule that called r.
		 *  You don't want the exact set of viable next tokens because the
		 *  input might just be missing a token--you might consume the
		 *  rest of the input looking for one of the missing tokens.
		 *
		 *  Consider grammar:
		 *
		 *  a : '[' b ']'
		 *    | '(' b ')'
		 *    ;
		 *  b : c '^' INT ;
		 *  c : ID
		 *    | INT
		 *    ;
		 *
		 *  At each rule invocation, the set of tokens that could follow
		 *  that rule is pushed on a stack.  Here are the various "local"
		 *  follow sets:
		 *
		 *  FOLLOW(b1_in_a) = FIRST(']') = ']'
		 *  FOLLOW(b2_in_a) = FIRST(')') = ')'
		 *  FOLLOW(c_in_b) = FIRST('^') = '^'
		 *
		 *  Upon erroneous input "[]", the call chain is
		 *
		 *  a -> b -> c
		 *
		 *  and, hence, the follow context stack is:
		 *
		 *  depth  local follow set     after call to rule
		 *    0         <EOF>                    a (from main())
		 *    1          ']'                     b
		 *    3          '^'                     c
		 *
		 *  Notice that ')' is not included, because b would have to have
		 *  been called from a different context in rule a for ')' to be
		 *  included.
		 *
		 *  For error recovery, we cannot consider FOLLOW(c)
		 *  (context-sensitive or otherwise).  We need the combined set of
		 *  all context-sensitive FOLLOW sets--the set of all tokens that
		 *  could follow any reference in the call chain.  We need to
		 *  resync to one of those tokens.  Note that FOLLOW(c)='^' and if
		 *  we resync'd to that token, we'd consume until EOF.  We need to
		 *  sync to context-sensitive FOLLOWs for a, b, and c: {']','^'}.
		 *  In this case, for input "[]", LA(1) is in this set so we would
		 *  not consume anything and after printing an error rule c would
		 *  return normally.  It would not find the required '^' though.
		 *  At this point, it gets a mismatched token error and throws an
		 *  exception (since LA(1) is not in the viable following token
		 *  set).  The rule exception handler tries to recover, but finds
		 *  the same recovery set and doesn't consume anything.  Rule b
		 *  exits normally returning to rule a.  Now it finds the ']' (and
		 *  with the successful match exits errorRecovery mode).
		 *
		 *  So, you cna see that the parser walks up call chain looking
		 *  for the token that was a member of the recovery set.
		 *
		 *  Errors are not generated in errorRecovery mode.
		 *
		 *  ANTLR's error recovery mechanism is based upon original ideas:
		 *
		 *  "Algorithms + Data Structures = Programs" by Niklaus Wirth
		 *
		 *  and
		 *
		 *  "A note on error recovery in recursive descent parsers":
		 *  http://portal.acm.org/citation.cfm?id=947902.947905
		 *
		 *  Later, Josef Grosch had some good ideas:
		 *
		 *  "Efficient and Comfortable Error Recovery in Recursive Descent
		 *  Parsers":
		 *  ftp://www.cocolab.com/products/cocktail/doca4.ps/ell.ps.zip
		 *
		 *  Like Grosch I implemented local FOLLOW sets that are combined
		 *  at run-time upon error to avoid overhead during parsing.
		 */
		protected function computeErrorRecoverySet():BitSet {
			return combineFollows(false);
		}
	
		/** Compute the context-sensitive FOLLOW set for current rule.
		 *  This is set of token types that can follow a specific rule
		 *  reference given a specific call chain.  You get the set of
		 *  viable tokens that can possibly come next (lookahead depth 1)
		 *  given the current call chain.  Contrast this with the
		 *  definition of plain FOLLOW for rule r:
		 *
		 *   FOLLOW(r)={x | S=>*alpha r beta in G and x in FIRST(beta)}
		 *
		 *  where x in T* and alpha, beta in V*; T is set of terminals and
		 *  V is the set of terminals and nonterminals.  In other words,
		 *  FOLLOW(r) is the set of all tokens that can possibly follow
		 *  references to r in *any* sentential form (context).  At
		 *  runtime, however, we know precisely which context applies as
		 *  we have the call chain.  We may compute the exact (rather
		 *  than covering superset) set of following tokens.
		 *
		 *  For example, consider grammar:
		 *
		 *  stat : ID '=' expr ';'      // FOLLOW(stat)=={EOF}
		 *       | "return" expr '.'
		 *       ;
		 *  expr : atom ('+' atom)* ;   // FOLLOW(expr)=={';','.',')'}
		 *  atom : INT                  // FOLLOW(atom)=={'+',')',';','.'}
		 *       | '(' expr ')'
		 *       ;
		 *
		 *  The FOLLOW sets are all inclusive whereas context-sensitive
		 *  FOLLOW sets are precisely what could follow a rule reference.
		 *  For input input "i=(3);", here is the derivation:
		 *
		 *  stat => ID '=' expr ';'
		 *       => ID '=' atom ('+' atom)* ';'
		 *       => ID '=' '(' expr ')' ('+' atom)* ';'
		 *       => ID '=' '(' atom ')' ('+' atom)* ';'
		 *       => ID '=' '(' INT ')' ('+' atom)* ';'
		 *       => ID '=' '(' INT ')' ';'
		 *
		 *  At the "3" token, you'd have a call chain of
		 *
		 *    stat -> expr -> atom -> expr -> atom
		 *
		 *  What can follow that specific nested ref to atom?  Exactly ')'
		 *  as you can see by looking at the derivation of this specific
		 *  input.  Contrast this with the FOLLOW(atom)={'+',')',';','.'}.
		 *
		 *  You want the exact viable token set when recovering from a
		 *  token mismatch.  Upon token mismatch, if LA(1) is member of
		 *  the viable next token set, then you know there is most likely
		 *  a missing token in the input stream.  "Insert" one by just not
		 *  throwing an exception.
		 */
		protected function computeContextSensitiveRuleFOLLOW():BitSet {
			return combineFollows(true);
		}
	
		protected function combineFollows(exact:Boolean):BitSet {
			var top:int = state._fsp;
			var followSet:BitSet = new BitSet();
			for (var i:int=top; i>=0; i--) {
				var localFollowSet:BitSet = state.following[i];
				followSet.orInPlace(localFollowSet);
				if ( exact && !localFollowSet.member(TokenConstants.EOR_TOKEN_TYPE) ) {
					break;
				}
			}
			followSet.remove(TokenConstants.EOR_TOKEN_TYPE);
			return followSet;
		}
	
		/** Attempt to recover from a single missing or extra token.
		 *
		 *  EXTRA TOKEN
		 *
		 *  LA(1) is not what we are looking for.  If LA(2) has the right token,
		 *  however, then assume LA(1) is some extra spurious token.  Delete it
		 *  and LA(2) as if we were doing a normal match(), which advances the
		 *  input.
		 *
		 *  MISSING TOKEN
		 *
		 *  If current token is consistent with what could come after
		 *  ttype then it is ok to "insert" the missing token, else throw
		 *  exception For example, Input "i=(3;" is clearly missing the
		 *  ')'.  When the parser returns from the nested call to expr, it
		 *  will have call chain:
		 *
		 *    stat -> expr -> atom
		 *
		 *  and it will be trying to match the ')' at this point in the
		 *  derivation:
		 *
		 *       => ID '=' '(' INT ')' ('+' atom)* ';'
		 *                          ^
		 *  match() will see that ';' doesn't match ')' and report a
		 *  mismatched token error.  To recover, it sees that LA(1)==';'
		 *  is in the set of tokens that can follow the ')' token
		 *  reference in rule atom.  It can assume that you forgot the ')'.
		 */
		public function recoverFromMismatchedToken(input:IntStream,
											   e:RecognitionException,
											   ttype:int,
											   follow:BitSet):void {	
			// if next token is what we are looking for then "delete" this token
			if ( input.LA(2)==ttype ) {
				reportError(e);
				beginResync();
				input.consume(); // simply delete extra token
				endResync();
				input.consume(); // move past ttype token as if all were ok
				return;
			}
			if ( !recoverFromMismatchedElement(input,e,follow) ) {
				throw e;
			}
		}
	
		public function recoverFromMismatchedSet(input:IntStream,
											 e:RecognitionException,
											 follow:BitSet):RecognitionException
		{
			// TODO do single token deletion like above for Token mismatch
			if ( !recoverFromMismatchedElement(input,e,follow) ) {
				throw e;
			}
			else {
			    // Return the exception back so it can be throws by caller, avoid exception
			    // declaration variable and compiler warnings.
			    return e;
			}
		}
	
		/** This code is factored out from mismatched token and mismatched set
		 *  recovery.  It handles "single token insertion" error recovery for
		 *  both.  No tokens are consumed to recover from insertions.  Return
		 *  true if recovery was possible else return false.
		 */
		protected function recoverFromMismatchedElement(input:IntStream,
													   e:RecognitionException,
													   follow:BitSet):Boolean
		{
			if ( follow==null ) {
				// we have no information about the follow; we can only consume
				// a single token and hope for the best
				return false;
			}
			//System.out.println("recoverFromMismatchedElement");
			// compute what can follow this grammar element reference
			if ( follow.member(TokenConstants.EOR_TOKEN_TYPE) ) {
				var viableTokensFollowingThisRule:BitSet =
					computeContextSensitiveRuleFOLLOW();
				follow = follow.or(viableTokensFollowingThisRule);
				follow.remove(TokenConstants.EOR_TOKEN_TYPE);
			}
			// if current token is consistent with what could come after set
			// then it is ok to "insert" the missing token, else throw exception
			//System.out.println("viable tokens="+follow.toString(getTokenNames())+")");
			if ( follow.member(input.LA(1)) ) {
				//System.out.println("LT(1)=="+input.LT(1)+" is consistent with what follows; inserting...");
				reportError(e);
				return true;
			}
			//System.err.println("nothing to do; throw exception");
			return false;
		}
	
		// GMS: Renamed from consumeUntil to consumeUntilToken
		public function consumeUntilToken(input:IntStream, tokenType:int):void {
			//System.out.println("consumeUntil "+tokenType);
			var ttype:int = input.LA(1);
			while (ttype != TokenConstants.EOF && ttype != tokenType) {
				input.consume();
				ttype = input.LA(1);
			}
		}
	
		/** Consume tokens until one matches the given token set */
		public function consumeUntil(input:IntStream, bitSet:BitSet):void {
			//System.out.println("consumeUntil("+set.toString(getTokenNames())+")");
			var ttype:int = input.LA(1);
			while (ttype != TokenConstants.EOF && !bitSet.member(ttype) ) {
				//System.out.println("consume during recover LA(1)="+getTokenNames()[input.LA(1)]);
				input.consume();
				ttype = input.LA(1);
			}
		}
	
		/** Push a rule's follow set using our own hardcoded stack */
		protected function pushFollow(fset:BitSet):void {
			state.following[++state._fsp] = fset;
		}
	
		public function get backtrackingLevel():int {
			return state.backtracking;
		}
	
		/** Used to print out token names like ID during debugging and
		 *  error reporting.  The generated parsers implement a method
		 *  that overrides this to point to their String[] tokenNames.
		 */
		public function get tokenNames():Array {
			return null;
		}
	
		/** For debugging and other purposes, might want the grammar name.
		 *  Have ANTLR generate an implementation for this method.
		 */
		public function get grammarFileName():String {
			return null;
		}
	
		/** A convenience method for use most often with template rewrites.
		 *  Convert a List<Token> to List<String>
		 */
		public function toStrings(tokens:Array):Array {
			if ( tokens==null ) return null;
			var strings:Array = new Array(tokens.length);
			for (var i:int = 0; i<tokens.length; i++) {
				strings.push(tokens[i].text);
			}
			return strings;
		}
	
		/** Given a rule number and a start token index number, return
		 *  MEMO_RULE_UNKNOWN if the rule has not parsed input starting from
		 *  start index.  If this rule has parsed input starting from the
		 *  start index before, then return where the rule stopped parsing.
		 *  It returns the index of the last token matched by the rule.
		 *
		 *  For now we use a hashtable and just the slow Object-based one.
		 *  Later, we can make a special one for ints and also one that
		 *  tosses out data after we commit past input position i.
		 * 
		 * GMS: converted this to use Associate Arrays for ruleMemos
		 */
		public function getRuleMemoization(ruleIndex:int, ruleStartIndex:int):int {
			if ( state.ruleMemo[ruleIndex]==null ) {
				state.ruleMemo[ruleIndex] = new Array();
			}
			var stopIndexI:String =	state.ruleMemo[ruleIndex][new String(ruleStartIndex)];
			if ( stopIndexI == null ) {
				return MEMO_RULE_UNKNOWN;
			}
			return int(stopIndexI);
		}
	
		/** Has this rule already parsed input at the current index in the
		 *  input stream?  Return the stop token index or MEMO_RULE_UNKNOWN.
		 *  If we attempted but failed to parse properly before, return
		 *  MEMO_RULE_FAILED.
		 *
		 *  This method has a side-effect: if we have seen this input for
		 *  this rule and successfully parsed before, then seek ahead to
		 *  1 past the stop token matched for this rule last time.
		 */
		public function alreadyParsedRule(input:IntStream, ruleIndex:int):Boolean {
			var stopIndex:int = getRuleMemoization(ruleIndex, input.index);
			if ( stopIndex==MEMO_RULE_UNKNOWN ) {
				return false;
			}
			if ( stopIndex==MEMO_RULE_FAILED ) {
				//System.out.println("rule "+ruleIndex+" will never succeed");
				state.failed=true;
			}
			else {
				//System.out.println("seen rule "+ruleIndex+" before; skipping ahead to @"+(stopIndex+1)+" failed="+failed);
				input.seek(stopIndex+1); // jump to one past stop token
			}
			return true;
		}
	
		/** Record whether or not this rule parsed the input at this position
		 *  successfully.  Use a standard java hashtable for now.
		 */
		public function memoize(input:IntStream,
							ruleIndex:int,
							ruleStartIndex:int):void
		{
			var stopTokenIndex:int = state.failed ? MEMO_RULE_FAILED : input.index - 1;
			if ( state.ruleMemo[ruleIndex]!=null ) {
				state.ruleMemo[ruleIndex].put(
					new String(ruleStartIndex), new String(stopTokenIndex)
				);
			}
		}
	
		/** return how many rule/input-index pairs there are in total.
		 *  TODO: this includes synpreds. :(
		 */
		public function getRuleMemoizationCacheSize():int {
			var n:int = 0;
			for (var i:int = 0; state.ruleMemo!=null && i < state.ruleMemo.length; i++) {
				var ruleMap:Array = state.ruleMemo[i];
				if ( ruleMap!=null ) {
					n += ruleMap.length; // how many input indexes are recorded?
				}
			}
			return n;
		}
	
	    // GMS : renamed traceInSymbol from traceIn
		public function traceInSymbol(ruleName:String, ruleIndex:int, inputSymbol:Object):void  {
			trace("enter "+ruleName+" "+inputSymbol);
			if ( state.failed ) {
				trace(" failed="+state.failed);
			}
			if ( state.backtracking>0 ) {
				trace(" backtracking="+state.backtracking);
			}
			trace();
		}
	
	    // GMS : renamd traceOutSymble from traceOut
		public function traceOutSymbol(ruleName:String,
							  ruleIndex:int,
							  inputSymbol:Object):void
		{
			trace("exit "+ruleName+" "+inputSymbol);
			if ( state.failed ) {
				trace(" failed="+state.failed);
			}
			if ( state.backtracking>0 ) {
				trace(" backtracking="+state.backtracking);
			}
			trace();
		}
	
		/** A syntactic predicate.  Returns true/false depending on whether
		 *  the specified grammar fragment matches the current input stream.
		 *  This resets the failed instance var afterwards.
		public boolean synpred(IntStream input, GrammarFragmentPtr fragment) {
			//int i = input.index();
			//System.out.println("begin backtracking="+backtracking+" @"+i+"="+((CommonTokenStream)input).LT(1));
			backtracking++;
			beginBacktrack(backtracking);
			int start = input.mark();
			try {fragment.invoke();}
			catch (RecognitionException re) {
				System.err.println("impossible: "+re);
			}
			boolean success = !failed;
			input.rewind(start);
			endBacktrack(backtracking, success);
			backtracking--;
			//System.out.println("end backtracking="+backtracking+": "+(failed?"FAILED":"SUCCEEDED")+" @"+input.index()+" should be "+i);
			failed=false;
			return success;
		}
		 */
	}
}