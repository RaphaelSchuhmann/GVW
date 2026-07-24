package com.gvw.gvwbackend.model;

import java.util.List;

public interface TextDocument {
  List<TextEditorBlock> getContents();
}
