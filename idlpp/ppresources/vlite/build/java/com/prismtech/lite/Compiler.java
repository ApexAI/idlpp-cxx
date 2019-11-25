package com.prismtech.lite;

import java.io.*;
import java.util.*;

import org.antlr.v4.runtime.*;

import com.prismtech.idt.imports.idl.preprocessor.*;
import com.prismtech.lite.parser.*;
import com.prismtech.lite.generator.GenVisitor;
import com.prismtech.lite.compilers.IdlcCmdOptions;

public class Compiler
{
  private static class PreProcAwareListener extends BaseErrorListener
  {
    public void syntaxError
    (
      Recognizer<?,?> recognizer,
      Object wrongSymbol,
      int line,
      int column,
      String msg,
      RecognitionException e
    )
    {
      System.err.println
        ("Error: At " + params.linetab.getRealPosition (line) + ", " + msg);
    }
  }

  public static void run (IdlcCmdOptions opts)
  {
    if (opts.version)
    {
      version ();
      return;
    }

    try
    {
      if (!LicenseMgr.checkout ("LITE_IDLC"))
      {
        System.exit (1);
      }

      String pathSep = System.getProperty ("file.separator");
      String outpath = (opts.outputdir == null) ? "" : opts.outputdir + pathSep;
      String fileRoot;

      IIdlPreprocessor preprocessor = IdlPreprocessorFactory.create ();
      IIdlPreprocessorStatus ppstatus;
      Map<File, List<File>> ppdependencies = new TreeMap<File, List<File>> ();
      CharArrayWriter ppresult = new CharArrayWriter ();
      IIdlPreprocessorParams ppparams = preprocessor.createPreprocessorParams ();
      for (String s : opts.includes)
      {
        ppparams.addIncludeDirectory (new File (s));
      }
      for (Map.Entry <String, String> macro : opts.macros.entrySet ())
      {
        ppparams.addMacroDefinition (macro.getKey (), macro.getValue ());
      }

      for (String arg : opts.files)
      {
        params = new IdlParams (opts);
        if (!arg.endsWith (".idl"))
        {
          arg += ".idl";
        }
        if (System.getProperty ("file.separator").equals ("\\"))
        {
          /* Preprocessor does this conversion on Windows, so we do the same */
          arg = arg.replace ('\\', '/');
        }

        File idl = new File(arg);
        if (idl.exists() && idl.isFile())
        {
          if (!opts.quiet && !opts.pponly)
          {
            System.out.println ("Compiling " + idl.getPath ());
          }
        }
        else
        {
          System.err.println
            ("Input IDL file " + idl.getPath () + " is not valid");
          licenseCheckAndExit (1);
        }
        fileRoot =
          idl.getName ().substring (0, idl.getName ().lastIndexOf ('.'));

        ppstatus =
          preprocessor.preprocess (ppparams, idl, ppresult, ppdependencies);

        if (!ppstatus.isOK ())
        {
          System.err.println ("Error: At " + ppstatus.getFilename () + ":" + ppstatus.getLine () + ", " + ppstatus.getMessage ());
          licenseCheckAndExit(1);
        }
        if (opts.pponly)
        {
          System.out.println (ppresult.toCharArray ());
          licenseCheckAndExit (0);
        }

        ANTLRInputStream input =
          new ANTLRInputStream (ppresult.toCharArray(), ppresult.size ());
        Lexer lexer = new IDLLexer (input);
        CommonTokenStream tokens = new CommonTokenStream (lexer);
        tokens.fill ();

        if (opts.dumptokens)
        {
          List<Token> tlist = tokens.getTokens ();
          Iterator<Token> it = tlist.iterator ();
          Token t;
          while (it.hasNext ())
          {
            t = it.next ();
            if (t.getChannel() == Token.DEFAULT_CHANNEL)
            {
              System.out.println (t.getText ());
            }
          }
          licenseCheckAndExit (0);
        }

        params.linetab = new LineTable
        (
          arg.replace ("//", "/"),
          tokens.getTokens ().iterator (),
          params.forcpp
        );
        IDLParser parser = new IDLParser (tokens);
        parser.removeErrorListeners ();
        parser.addErrorListener (new PreProcAwareListener ());
        try
        {
          ParserRuleContext tree = (ParserRuleContext)parser.specification ();
          if (parser.getNumberOfSyntaxErrors () != 0)
          {
            licenseCheckAndExit (1);
          }
          if (opts.dumptree)
          {
            if (java.awt.GraphicsEnvironment.isHeadless ())
            {
              System.out.println (tree.toStringTree (parser));
            }
            else
            {
              javax.swing.JDialog jd = tree.inspect (parser).get ();
              jd.setVisible (false);
              jd.setModalityType (java.awt.Dialog.ModalityType.APPLICATION_MODAL);
              jd.setVisible (true);
            }
            licenseCheckAndExit(0);
          }

          params.symtab = new SymbolTable ();
          GenSymbolTable gst = new GenSymbolTable (params);
          gst.visit (tree);
          if (gst.getErrorCount () != 0)
          {
            licenseCheckAndExit (1);
          }
          if (gst.unresolvedSymbols ())
          {
            licenseCheckAndExit (1);
          }

          if (opts.dumpsymbols)
          {
            System.out.println ("Symbol table pass complete, symbols are:");
            params.symtab.dump ();
            licenseCheckAndExit (0);
          }

          params.basename = fileRoot;

          if (params.forcpp)
          {
            fileRoot = fileRoot.concat ("-cyclone");
          }
          try
          {
            GenVisitor codegenh = new GenVisitor (params, "com/prismtech/lite/templates/h/templates.stg");
            codegenh.visit (tree);
            codegenh.writeToFile (outpath + fileRoot + ".h");

            if (!params.notopics)
            {
              params.quiet = true;
              GenVisitor codegenc = new GenVisitor (params, "com/prismtech/lite/templates/c/templates.stg");
              codegenc.visit (tree);
              codegenc.writeToFile (outpath + fileRoot + ".c");
            }
          }
          catch (IOException x)
          {
            System.err.format("IOException: %s%n", x);
            licenseCheckAndExit (1);
          }
        }
        catch (RecognitionException r)
        {
          r.printStackTrace ();
          licenseCheckAndExit (1);
        }
      }
    }
    catch (Exception e)
    {
      e.printStackTrace ();
    }
    finally
    {
      LicenseMgr.checkin ();
    }
  }

  private static void licenseCheckAndExit (int exitStatus)
  {
    LicenseMgr.checkin ();
    System.exit (exitStatus);
  }

  private static void version ()
  {
    System.out.print ("Vortex Lite DDS ");
    System.out.println ("C IDL Compiler v" + Version.version);
  }

  private static IdlParams params;
}
