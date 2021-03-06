package com.prismtech.lite.generator;

import com.prismtech.lite.ScopedName;
import java.util.*;

public interface Type
{
  public ArrayList <String> getMetaOp (String myname, String structname);
  public String getSubOp ();
  public String getOp ();
  public String getCType ();
  public void getXML (StringBuffer str, ModuleContext mod);
  public void populateDeps (Set <ScopedName> depset, NamedType current);
  public boolean depsOK (Set <ScopedName> deps);
  public void makeKeyField ();
  public boolean isKeyField ();
  public long getKeySize ();
  public int getMetaOpSize ();
  public Alignment getAlignment ();
  public Type dup ();
  public boolean containsUnion ();
}

