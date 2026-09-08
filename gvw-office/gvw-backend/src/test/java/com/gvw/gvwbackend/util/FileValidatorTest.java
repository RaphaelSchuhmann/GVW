package com.gvw.gvwbackend.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class FileValidatorTest {

  @InjectMocks private FileValidator fileValidator;

  @Test
  void isSafe_NullMultipartFile_ReturnsFalse() {
    assertFalse(fileValidator.isSafe(null));
  }

  @Test
  void isSafe_NullFilename_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn(null);

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_NoExtension_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("foobar");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_DotOnly_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("file.");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_DotExeOnly_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn(".exe");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Exe_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.exe");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Bat_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.bat");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Cmd_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.cmd");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Sh_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.sh");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Bin_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.bin");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Dll_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.dll");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_So_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.so");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Jar_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.jar");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Class_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.class");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Bmp_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.bmp");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Tiff_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.tiff");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Webp_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.webp");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Iso_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.iso");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Zip_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.zip");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_7z_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.7z");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Rar_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.rar");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_PdfUppercase_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.PDF");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_MusicXmlMixedCase_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("song.MuSiCxMl");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_PdfExe_ReturnsFalse() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("foo.pdf.exe");

    assertFalse(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_ExePdf_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("foo.exe.pdf");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Pdf_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.pdf");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Png_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.png");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Jpg_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.jpg");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Jpeg_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.jpeg");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Gif_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.gif");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Mp3_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.mp3");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Wav_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.wav");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Midi_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.midi");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Mid_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.mid");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Xml_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.xml");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Musicxml_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.musicxml");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Mxl_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.mxl");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Mscz_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.mscz");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Mscx_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.mscx");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Sib_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.sib");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Musx_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.musx");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Cap_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.cap");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Capx_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.capx");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Gp_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.gp");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Gp5_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.gp5");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Gp3_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.gp3");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Gp4_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.gp4");

    assertTrue(fileValidator.isSafe(mockFile));
  }

  @Test
  void isSafe_Gpx_ReturnsTrue() {
    MultipartFile mockFile = mock(MultipartFile.class);
    when(mockFile.getOriginalFilename()).thenReturn("test.gpx");

    assertTrue(fileValidator.isSafe(mockFile));
  }
}
