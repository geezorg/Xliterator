<img src="src/main/resources/images/Xliterator.png" width="200"/>

# Xliterator

A general purpose  [ICU](http://site.icu-project.org/) based transliteration utility.

## Summary
Xliterator is a tool that makes it easy to apply the library of 350+ [CLDR](https://github.com/unicode-org/cldr) transliteration definitions
for the writing conventions around the world. A text window allows for the conversion of text samples, another window allows you to load Microsoft
Word files, select target fonts, and transliterate the text under those fonts into a target system.  Xliterator provides editor windows to
view and modify the bundled CLDR transliterations and makes it easy to compose entirely new transliteation systems. Finally, a syntax highlighting
editor allows you to set syntax coloring anyway you desire.

## Download
* [Direct download of the v0.6 beta here.](https://github.com/geezorg/Xliterator/releases/download/v0.6.0/Xliterator-0.6.0-beta-full-gui.jar)
* [Read the notes on the latest updates.](https://github.com/geezorg/Xliterator/releases/tag/v0.6.0-beta)


## Usage
The Xliterator includes the full [CLDR 36 transforms collection](https://github.com/unicode-org/cldr/tree/master/common/transforms).  These packaged transliteration files may be selected via the **Script In** and  **Script Out** menus.  Where applicable, submenus will appear to select a given writing convention for a script.  For example the Azerbaijani utilization of Cyrillic script verses Mongolian. Once a system has been selected, it will be applied for both text and file conversions. The **Script In** and **Script Out** selections may be set as the defaults for future sessions by selecting the menu item **Preferences &gt; Save Transliteration Selection**.


### Convert Text Tab
After making the target **Script In** and **Script Out** selections, text can be converted in the editor windows.  Text may be typed in directly or pasted in from another source. By default, text will convert dynamically as you type.  Unchecking the "Auto Convert" checkbox turns this behaviro off.  The *forward* direction with the down arrow button (⬇).  If supported by the selected transliteration system, reverse transliteration can be performed from the lower to upper windows using the up arrow button (⬆).

The **Preferences &gt; Convert Case** menu item adds a further text conversion option wherebey the transliterated output can be set to "UPPERCASE", "lowercaase", or "Title Case" as applicable for the output script.

If text appears as emtpy rectangles (&#x25af;) it means that the window font does not support source or target script.  Both the upper and lower windows have font selection menus to change the font as required for the source or target script. Font sizes may also be increased or decreased as needed.  Font selections may be saved for future sessions by selecting the menu item **Preferences &gt; Save Font Selections**.

<img src="doc/Xliterator-ConvertText-v0.6.png" width="800" border="1" alt="v0.6 Convert Text"/>


### Convert Files Tab
The **File Converter** tab is hidden by default.  It may be revealed by selecting the menu **Tabs &gt; File Converter**. Note that any tab appearing in Xliterator will also be listed under the **Tabs** menu which in turn allows you to hide and show tabs as needed.

Selected files will be converted under the selected transliteration system (or editor if chosen).  Transliteration is only performed on targeted fonts selected under the **Document Fonts** menu.  The **Output Font** will be applied to converted text, replacing the original targetted font.  The selected source files are not modified, output will appear in a new file using the source file name with the output script name appended.

By default transliterated text will replace the source text under a target font.  The **Append Output?** menu offers the option to append the transliterated  text instead. Transliterated text may be appended  on a new line or on the same line as the original text separated with a space.  Under both appending options, the transliterated text may be optionally also be enclosed in parentheses.

<img src="doc/Xliterator-ConvertFiles-v0.6.png" width="800" border="1" alt="v0.6 Convert Files"/>


### Transliteraton Mapping Editor Tab
The mapping editor offers syntax highlighting for ICU files in either XML or text formats.  An external file may be loaded
in the usual way via **File &gt; Open ICU File...** .  An internal transliteration system selected via the **Script In** , **Script Out** and **Variant** menus can be loaded file **File &gt; Load Selected Transliteration** .  Once a file is loaded, the **Script In** menu will be set to the **Use Editor** option.

<img src="doc/Xliterator-MappingEditor-v0.6.png" width="800" border="1" alt="v0.6 Mapping Editor"/>

### Syntax Highlighting
The syntax coloring and styling used in the transliteration editor can be set with the Syntax Highlighter window.  The window can be opened by selecting the menu **Preferences &gt;gt; Edit Syntax Highlighting**.   Note that the *Background* and *Foreground* colors are also applied to the **Convert  Text** tab.  Highlighting changed can be tested by clicking the **Apply** button and later saved as the default by clicking the **Save** button.  Under Microsoft Windows, saved syntax highlighting definitions are stored in your `Users\&lt;User&gt;\AppData\Local\Xliterator` folder.  Under OSX and Linux operating systems the definitions are stored in your `~/.config/xlit` directory.  In both cases the syntax highlighting definitions will be saved in the file `icu-highlighting.css` which may also be updated in your favorite editor.


---

### Troubleshooting

If nothing happens when you double click the "Xliterator-0.6.0-beta-full-gui.jar" file, and you are certain that
Java is installed on your Windows system, you likely need to fix the Window's registry.  Try downloading and launching
[Jarfix](https://johann.loefflmann.net/en/software/jarfix/) to fix the registry.



### Issues
Issues can be reported to the author directly, or via the GitHub [issues tracker](https://github.com/geezorg/Xliterator/issues)
for the project.

