- Note from Charles -
Ok, I think I have everything covered for community involvement:
If you contribute make sure to toss your name into the AUTHORS.txt file
for the proper directory. Make sure to toss in commenting and supply JavaDoc
for Methods / Classes. Otherwise, have fun with it!

Structure:
-layer.xml contains the FAC side of this project.
-DarkMonkeyLookAndFeel contains the LAF side of this project.
-Installer is responsible for making sure LAF side and unregistered fonts are
    available for each instance of netbeans.
-DMUtils contains resource/file methods for base IO or resource transformations
-DarkMonkeyValidator is a placeholder launchpoint snippet for conversion from
    nbm format to a form that would integrate with an SDK downloadable build
    (it is unnecessary as-is. Would replace Installer, functionally)
-DarkMonkey.theme is the desired way to use nimrodlf (unimplemented at current time)
-DarkMonkeyIconFactory produces images/icons for the small/fixed size level
    -Tree.expandedIcon is overridden
    -Tree.collapsedIcon is overridden
    - more can be added
-Bundle.properties contains one of the cooler module description pages, ever*.
    -* COOLEST. DESCRIPTION. EVER. (I'm cereal! / lol! /omg!)

Directory Structure:
org.jme3.netbeans.plaf.darkmonkey is considered root for the relative path calls
that you will see in code. *Especially* if DMUtils is involved.
    -ext/ or external, currently contains nimrodlf.jar and nimrodlf_src.zip
    -fac/ or FontsColors, contains the FontsColors.xml files for layer.xml to
        place into netbean's xmlfilesytem.
    -fac/AnnotationTypes/ contains the Annotation.xml files for layer.xml to
        place into netbean's xmlfilesytem. *Unimplemented* Only if the sidebar
        icons for breakpoints and such should be replaced.
    -fac/AnnotationTypes/icons *Unimplemented* *Does not exist yet* see above.
    -fonts/ *unimplented* contains the physical .ttf fonts used by the LAF and FAC sides.
    -icons/ contains image resources for LAF side.


Current Todo List:
-fix bug in src for nimrodlf.jar rebuild in ext/
    - Bug has to do with GridBagLayout and other such stuff where preferredSize
      is derived.
-add font check/load code for fonts/. see JunkSnippet.txt for some proto work.