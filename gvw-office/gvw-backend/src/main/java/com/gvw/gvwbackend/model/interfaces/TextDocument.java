package com.gvw.gvwbackend.model.interfaces;

import com.gvw.gvwbackend.model.TextEditorBlock;

import java.util.List;

public interface TextDocument {
  List<TextEditorBlock> getContents();
}
