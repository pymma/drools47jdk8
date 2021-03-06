package org.drools.rule.builder.dialect.mvel;

import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.drools.Cheese;
import org.drools.RuleBase;
import org.drools.RuleBaseFactory;
import org.drools.WorkingMemory;
import org.drools.base.ClassObjectType;
import org.drools.base.DefaultKnowledgeHelper;
import org.drools.base.mvel.MVELConsequence;
import org.drools.base.mvel.MVELDebugHandler;
import org.drools.common.AgendaItem;
import org.drools.common.InternalFactHandle;
import org.drools.common.PropagationContextImpl;
import org.drools.compiler.Dialect;
import org.drools.compiler.DialectConfiguration;
import org.drools.compiler.DrlParser;
import org.drools.compiler.DroolsParserException;
import org.drools.compiler.PackageBuilder;
import org.drools.compiler.PackageBuilderConfiguration;
import org.drools.lang.descr.PackageDescr;
import org.drools.lang.descr.RuleDescr;
import org.drools.reteoo.ReteTuple;
import org.drools.rule.Declaration;
import org.drools.rule.Package;
import org.drools.rule.Pattern;
import org.drools.rule.Rule;
import org.drools.rule.builder.RuleBuildContext;
import org.drools.rule.builder.RuleBuilder;
import org.drools.spi.ObjectType;
import org.drools.spi.PatternExtractor;
import org.mvel.ExpressionCompiler;
import org.mvel.ParserContext;

public class MVELConsequenceBuilderTest extends TestCase {

    public void setUp() {
    }

    public void testSimpleExpression() throws Exception {
        final Package pkg = new Package( "pkg1" );
        final RuleDescr ruleDescr = new RuleDescr( "rule 1" );
        ruleDescr.setConsequence( "modify (cheese) {price = 5 };\nretract (cheese)" );

        PackageBuilder pkgBuilder = new PackageBuilder( pkg );
        final PackageBuilderConfiguration conf = pkgBuilder.getPackageBuilderConfiguration();
        MVELDialect mvelDialect = ( MVELDialect ) pkgBuilder.getDialectRegistry().getDialect( "mvel" );

        final InstrumentedBuildContent context = new InstrumentedBuildContent( conf,
                                                                               pkg,
                                                                               ruleDescr,
                                                                               pkgBuilder.getDialectRegistry(),
                                                                               mvelDialect );

        final InstrumentedDeclarationScopeResolver declarationResolver = new InstrumentedDeclarationScopeResolver();

        final ObjectType cheeseObjeectType = new ClassObjectType( Cheese.class );

        final Pattern pattern = new Pattern( 0,
                                             cheeseObjeectType );

        final PatternExtractor extractor = new PatternExtractor( cheeseObjeectType );

        final Declaration declaration = new Declaration( "cheese",
                                                         extractor,
                                                         pattern );
        final Map map = new HashMap();
        map.put( "cheese",
                 declaration );
        declarationResolver.setDeclarations( map );
        context.setDeclarationResolver( declarationResolver );

        final MVELConsequenceBuilder builder = new MVELConsequenceBuilder();
        builder.build( context );

        final RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        final WorkingMemory wm = ruleBase.newStatefulSession();

        final Cheese cheddar = new Cheese( "cheddar",
                                           10 );
        final InternalFactHandle f0 = (InternalFactHandle) wm.insert( cheddar );
        final ReteTuple tuple = new ReteTuple( f0 );

        final AgendaItem item = new AgendaItem( 0,
                                                tuple,
                                                10,
                                                new PropagationContextImpl(1, 1, null, null),
                                                context.getRule(),
                                                null );
        final DefaultKnowledgeHelper kbHelper = new DefaultKnowledgeHelper( wm );
        kbHelper.setActivation( item );
        context.getRule().getConsequence().evaluate( kbHelper,
                                                     wm );

        assertEquals( 5,
                      cheddar.getPrice() );
    }

    public void testKnowledgeHelper() {

    }

    public void testImperativeCodeError() throws Exception {
        final Package pkg = new Package( "pkg1" );
        final RuleDescr ruleDescr = new RuleDescr( "rule 1" );
        ruleDescr.setConsequence( "if (cheese.price == 10) { cheese.price = 5; }" );

        Properties properties = new Properties();
        properties.setProperty( "drools.dialect.default",
                                "mvel" );
        PackageBuilderConfiguration cfg1 = new PackageBuilderConfiguration( properties );
        
        PackageBuilder pkgBuilder = new PackageBuilder( pkg, cfg1 );
        final PackageBuilderConfiguration conf = pkgBuilder.getPackageBuilderConfiguration();
        MVELDialect mvelDialect = ( MVELDialect ) pkgBuilder.getDefaultDialect();

        final InstrumentedBuildContent context = new InstrumentedBuildContent( conf,
                                                                               pkg,
                                                                               ruleDescr,
                                                                               pkgBuilder.getDialectRegistry(),
                                                                               mvelDialect );

        final InstrumentedDeclarationScopeResolver declarationResolver = new InstrumentedDeclarationScopeResolver();

        final ObjectType cheeseObjeectType = new ClassObjectType( Cheese.class );

        final Pattern pattern = new Pattern( 0,
                                             cheeseObjeectType );

        final PatternExtractor extractor = new PatternExtractor( cheeseObjeectType );

        final Declaration declaration = new Declaration( "cheese",
                                                         extractor,
                                                         pattern );
        final Map map = new HashMap();
        map.put( "cheese",
                 declaration );
        declarationResolver.setDeclarations( map );
        context.setDeclarationResolver( declarationResolver );

        final MVELConsequenceBuilder builder = new MVELConsequenceBuilder();
        builder.build( context );

        final RuleBase ruleBase = RuleBaseFactory.newRuleBase();
        final WorkingMemory wm = ruleBase.newStatefulSession();

        final Cheese cheddar = new Cheese( "cheddar",
                                           10 );
        final InternalFactHandle f0 = (InternalFactHandle) wm.insert( cheddar );
        final ReteTuple tuple = new ReteTuple( f0 );

        final AgendaItem item = new AgendaItem( 0,
                                                tuple,
                                                10,
                                                null,
                                                context.getRule(),
                                                null );
        final DefaultKnowledgeHelper kbHelper = new DefaultKnowledgeHelper( wm );
        kbHelper.setActivation( item );
        try {
        context.getRule().getConsequence().evaluate( kbHelper,
                                                     wm );
            fail( "should throw an exception, as 'if' is not allowed" );
        } catch ( Exception e) {

        }

        assertEquals( 10,
                      cheddar.getPrice() );
    }

    /**
     * Just like MVEL command line, we can allow expressions to span lines, with optional ";"
     * seperating expressions. If its needed a ";" can be thrown in, but if not, a new line is fine.
     *
     * However, when in the middle of unbalanced brackets, a new line means nothing.
     *
     * @throws Exception
     */
    public void testLineSpanOptionalSemis() throws Exception {

        String simpleEx = "foo\nbar\nbaz";
        MVELConsequenceBuilder cons = new MVELConsequenceBuilder();
        assertEquals( "foo;\nbar;\nbaz",
                      MVELConsequenceBuilder.delimitExpressions( simpleEx ) );

        String ex = "foo (\n bar \n)\nbar;\nyeah;\nman\nbaby";
        assertEquals( "foo (\n bar \n);\nbar;\nyeah;\nman;\nbaby",
                      MVELConsequenceBuilder.delimitExpressions( ex ) );

        ex = "foo {\n bar \n}\nbar;   \nyeah;\nman\nbaby";
        assertEquals( "foo {\n bar \n};\nbar;   \nyeah;\nman;\nbaby",
                      MVELConsequenceBuilder.delimitExpressions( ex ) );

        ex = "foo [\n bar \n]\nbar;  x\nyeah();\nman[42]\nbaby;ca chiga;\nend";
        assertEquals( "foo [\n bar \n];\nbar;  x;\nyeah();\nman[42];\nbaby;ca chiga;\nend",
                      MVELConsequenceBuilder.delimitExpressions( ex ) );

        ex = "   \n\nfoo [\n bar \n]\n\n\nbar;  x\n  \nyeah();\nman[42]\nbaby;ca chiga;\nend";
        assertEquals( "   \n\nfoo [\n bar \n];\n\n\nbar;  x;\n  \nyeah();\nman[42];\nbaby;ca chiga;\nend",
                      MVELConsequenceBuilder.delimitExpressions( ex ) );

    }
    
    public void testMVELDebugSymbols() throws DroolsParserException {
        
        MVELDebugHandler.setDebugMode( true );
        
        try {
            final DrlParser parser = new DrlParser();
            final PackageDescr pkgDescr = parser.parse( new InputStreamReader( getClass().getResourceAsStream( "mvel_rule.drl" ) ) );

            // just checking there is no parsing errors
            Assert.assertFalse( parser.getErrors().toString(),
                                parser.hasErrors() );

            final Package pkg = new Package( "org.drools" );

            final RuleDescr ruleDescr = (RuleDescr) pkgDescr.getRules().get( 0 );

            final RuleBuilder builder = new RuleBuilder();

            final PackageBuilder pkgBuilder = new PackageBuilder( pkg );
            final PackageBuilderConfiguration conf = pkgBuilder.getPackageBuilderConfiguration();
            Dialect dialect = pkgBuilder.getDialectRegistry().getDialect( "mvel" );

            RuleBuildContext context = new RuleBuildContext( conf,
                                                             pkg,
                                                             ruleDescr,
                                                             pkgBuilder.getDialectRegistry(),
                                                             dialect );

            builder.build( context );

            Assert.assertTrue( context.getErrors().toString(),
                               context.getErrors().isEmpty() );

            final Rule rule = context.getRule();

            MVELConsequence mvelCons = (MVELConsequence) rule.getConsequence();
            String s = org.mvel.debug.DebugTools.decompile( mvelCons.getCompExpr() );

            int fromIndex = 0;
            int count = 0;
            while ( (fromIndex = s.indexOf( "DEBUG_SYMBOL",
                                            fromIndex + 1 )) > -1 ) {
                count++;
            }
            assertEquals( 4,
                          count );
        } finally {
            MVELDebugHandler.setDebugMode( false );
        }
        
    }

    public void testX() {
        String expr = "System.out.println( \"a1\" );\n" + 
                      "System.out.println( \"a2\" );\n" + 
                      "System.out.println( \"a3\" );\n" + 
                      "System.out.println( \"a4\" );\n";
   
        ExpressionCompiler compiler = new ExpressionCompiler(expr);
   
        ParserContext context = new ParserContext();
        context.addImport("System", System.class);
        context.setStrictTypeEnforcement(true);
        //context.setDebugSymbols( true );
        context.setSourceFile( "mysource" );
   
        compiler.setDebugSymbols( true );
        
        Serializable compiledExpression = compiler.compile(context);           
   
        String s = org.mvel.debug.DebugTools.decompile( compiledExpression );
   
        System.out.println("s "+s);
        
        int fromIndex=0;
        int count = 0;
        while ((fromIndex = s.indexOf( "DEBUG_SYMBOL", fromIndex+1 )) > -1) {
            count++;
        }
        assertEquals(4, count);      
   
      }
}

