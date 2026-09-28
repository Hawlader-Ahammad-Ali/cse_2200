package com.mindmap.export;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;

import java.awt.Color;

/**
 * Shared PDF styling constants and small helpers.
 * Uses OpenPDF's Font (Helvetica family — portable, no embedding needed).
 */
public final class PdfStyles {

    // =============================================================
    // Palette (matches the app's accent colors)
    // =============================================================

    public static final Color COLOR_TEXT       = new Color(44, 62, 80);      // #2C3E50
    public static final Color COLOR_MUTED      = new Color(107, 114, 128);   // #6B7280
    public static final Color COLOR_ACCENT     = new Color(26, 188, 156);    // #1ABC9C
    public static final Color COLOR_DANGER     = new Color(231, 76, 60);     // #E74C3C
    public static final Color COLOR_SUCCESS    = new Color(39, 174, 96);     // #27AE60
    public static final Color COLOR_WARNING    = new Color(243, 156, 18);    // #F39C12
    public static final Color COLOR_BORDER     = new Color(229, 231, 235);   // #E5E7EB

    // =============================================================
    // Fonts
    // =============================================================

    public static Font title()      { return new Font(Font.HELVETICA, 24, Font.BOLD,  COLOR_TEXT);   }
    public static Font subtitle()   { return new Font(Font.HELVETICA, 12, Font.NORMAL, COLOR_MUTED); }
    public static Font h1()         { return new Font(Font.HELVETICA, 16, Font.BOLD,  COLOR_TEXT);   }
    public static Font h2()         { return new Font(Font.HELVETICA, 13, Font.BOLD,  COLOR_TEXT);   }
    public static Font body()       { return new Font(Font.HELVETICA, 11, Font.NORMAL, COLOR_TEXT);   }
    public static Font bodyItalic() { return new Font(Font.HELVETICA, 11, Font.ITALIC, COLOR_MUTED);  }
    public static Font small()      { return new Font(Font.HELVETICA, 9,  Font.NORMAL, COLOR_MUTED);  }
    public static Font label()      { return new Font(Font.HELVETICA, 10, Font.BOLD,  COLOR_MUTED);   }
    public static Font accent()     { return new Font(Font.HELVETICA, 11, Font.BOLD,  COLOR_ACCENT);  }

    // =============================================================
    // Helpers
    // =============================================================

    public static Paragraph title(String text) {
        Paragraph p = new Paragraph(text, title());
        p.setSpacingAfter(4);
        return p;
    }

    public static Paragraph subtitle(String text) {
        Paragraph p = new Paragraph(text, subtitle());
        p.setSpacingAfter(12);
        return p;
    }

    public static Paragraph h1(String text) {
        Paragraph p = new Paragraph(text, h1());
        p.setSpacingBefore(14);
        p.setSpacingAfter(6);
        return p;
    }

    public static Paragraph h2(String text) {
        Paragraph p = new Paragraph(text, h2());
        p.setSpacingBefore(10);
        p.setSpacingAfter(4);
        return p;
    }

    public static Paragraph body(String text) {
        Paragraph p = new Paragraph(text == null ? "" : text, body());
        p.setSpacingAfter(6);
        p.setLeading(15);
        return p;
    }

    public static Paragraph muted(String text) {
        Paragraph p = new Paragraph(text == null ? "" : text, bodyItalic());
        p.setSpacingAfter(6);
        return p;
    }

    public static Paragraph labelValue(String label, String value) {
        Paragraph p = new Paragraph();
        p.add(new com.lowagie.text.Chunk(label + ":  ", label()));
        p.add(new com.lowagie.text.Chunk(value == null ? "—" : value, body()));
        p.setSpacingAfter(3);
        return p;
    }

    private PdfStyles() { }

    /** Wraps FontFactory to allow future custom fonts. Currently identity. */
    @SuppressWarnings("unused")
    private static Font custom(String name, float size, int style, Color color) {
        return FontFactory.getFont(name, size, style, color);
    }
}