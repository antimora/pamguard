PAMGUARD Beta Release Notes
===========================

[Version 0.01a 18/August/05](#_Toc312065299)

[Version 0.02a 16/December/05](#_Toc312065300)

[Version 0.03b 18/08/06](#_Toc312065301)

[Version 0.4b 29 December 2006](#_Toc312065302)

[Version 0.5b 31 August 2007](#_Toc312065303)

[Version 1.0Beta 22 Jan 2008](#_Toc312065304)

[Version 1.1Beta 22 May 2008](#_Toc312065305)

[Version 1.1.1 Beta 28 July 2008](#_Toc312065306)

[Version 1.2.0 Beta December 2008](#_Toc312065307)

[Version 1.2.01 Beta. December 2008](#_Toc312065308)

[Version 1.3.01 Beta. January 27 2009](#_Toc312065309)

[Version 1.3.02 Beta. February 2009](#_Toc312065310)

[Version 1.3.03 Beta March 2009](#_Toc312065311)

[Version 1.4.00 Beta June 2009](#_Toc312065312)

[Version 1.5.00 Beta - NI Support](#_Toc312065313)

[Version 1.5.01 Beta](#_Toc312065314)

[Version 1.6.00 Beta](#_Toc312065315)

[Version 1.7.00 Beta October 2009](#_Toc312065316)

[Version 1.7.02 Beta November 2009](#_Toc312065317)

[Version 1.8.00 Beta January 2010](#_Toc312065318)

[Version 1.8.01 Beta February 2010](#_Toc312065319)

[Version 1.8.01 Beta February 2010](#_Toc312065320)

[Version 1.9.00 Beta April 2010](#_Toc312065321)

[Version 1.9.01 Beta May 5 2010](#_Toc312065322)

[Version 1.10.00 Beta December 2010](#_Toc312065323)

[Version 1.11.01 Beta April 2012](#_Version_1.11.01_Beta)

[Version 1.11.02 Beta May 2012](#_Version_1.11.02_Beta)

[Version 1.12.00 Beta February 2013](#_Version_1.12.00_Beta)


## <a name="_Toc312065299"></a>Version 0.01a 18/August/05

First public release. 

## <a name="_Toc312065300"></a>Version 0.02a 16/December/05

First version with significant functionality. 

## <a name="_Toc312065301"></a>Version 0.03b 18/08/06

First version used in a real 'at sea' environment.

## <a name="_Toc312065302"></a>Version 0.4b 29 December 2006

Significant enhancements. 

A model manager and profiler, whereby users can add/remove
modules as required. 

Modules now include configurable click and whistle detectors
and Ishmael-type detectors and localisers. 

Information from above modules can be displayed on
configurable user displays which support real time scrolling spectrograms and
radar displays. Detection and localisation information can be optionally
displayed on the map display.Map enhancements include improved scrolling
whereby the user can click and drag to pan the area. PamGuard can now interface
with MySQL database servers and users can easily select which information is
logged. (This replaces the previous &#8220;flat-file&#8221; logging feature). A simulation
module allows virtual vocalising animals to be placed on the map to assist in
training and development. Extensive online user help has been added to PamGuard
Application

## <a name="_Toc312065303"></a><a name="_Toc312063948"></a>0.5b 31 August 2007 

Requires Java 6 Update 2
(http://java.com/en/download/manual.jsp)

Major new features include:

* ASIO sound card support 

* MS Access database access (up to Access 2003 verified)

* Configurations can be saved and re-called using settings files

* Module dependency wizard

* Air gun display

* AIS data interface/display

* Serial port support for NMEA data

* Spectra plugin display

* Static and "threading" hydrophone array configuration

## <a name="_Toc312065304"></a><a name="_Toc312063949"></a>1.0Beta 22 Jan 2008 

Pamguard starts two releases, core and beta release, this is the beta release

* 3D module

* Ishmael detection and localization methods

* Better display

* Multi animal click tracking with least squares fit calculation of
position on map

* Whistle detector multi channel tracking and localisation. 

* Sound playback

* Configuration storage to database

* Signal Patch Panel

* Default module naming and exclusive module naming

* Signal Amplifier

* IIRF (Butterworth and Chebychev) filters

* Offline file analysis

* Map (and other display) symbol keys

* Map Comments 

## <a name="_Toc312065305"></a>Version 1.1Beta 22 May 2008

* Asio Sound Card Multiple Channel Selection

* Viewer and Mixed mode operation

* Vessel Display

* Updated online help

## <a name="_Toc312065306"></a>Version 1.1.1 Beta 28 July 2008

**New Modules (not released to core)**

* video range tracking

* shaft angle readout using FluxgateWorld shaft encoders (used by video
range and beaked whale protocols)

* Beaked Whale Protocol (protocol to lay out randomised tracks around a
point  located from shore)

* Seismic veto code to detect seismic pulses and then veto out that pulse
from audio and spectrogram data streams

* whistle classifier Statistical species classification of whistle
detector data. 

* Land marks Fixed land marks to draw on map. 

**Updates**

* Click detector angle vetoes (can remove all clicks from a given angle,
or  sets of angles)

* Click detector pause scrolling to view individual click waveforms and
spectra.

* Improved help and activation of help buttons on dialogs. 

* Support for GPS GGA data

* Offline file analysis supports wider range of file name date formats

* Map has improved options to select what is drawn and how long overlays
last for. 

* User selectable symbols for many detectors drawing on the map. 

**Bug Fixes**

* Look up table fix for unpacking of AIS data. 

* Force English language Locale so that dialogs don't get confused by
number formats using "," instead of "."

## <a name="_Toc312065307"></a>Version 1.2.0 Beta December 2008

This release contains many bug fixes from the PAMGUARD
industry field trial which took place in the Gulf of Mexico on the RV New
Venture in November 2008.

**Bug Fixes**

* Map. Full drawing of map base (contours & grid lines) when window is
resized.

* Click Detector. Display and detection continue after click detection
parameter changes

* ASIO Sound acquisition. Was not returning channels in correct order.
 Now fixed.

* National Instruments sound acquisition. An NI interface has now been
written  for Windows. 

* NMEA, AIS and GPS modules. No longer crash in response to
incomplete  NMEA strings

* Spectrum display (plug in module for spectrogram). Now correctly saves
 settings between runs. 

* Dialogs (all modules). Have improved scaling and packing of dialogs when
channels or textual content change. 

* Model viewer. Have stopped it from jumping on top of the main PAMGUARD
 display when dialogs are closed. 

* General problems with timing solved (sound cards not always running at
the speed they should run at). Large improvements in overall PAMGUARD
reliability. 

* Airgun display is now correctly updating using either GPS or AIS data.

**Outstanding bugs**

* National Instruments sound acquisition. Hangs occasionally. Requires
restart  of PAMGUARD to get going again. 

* Click Detector. Click train detector uses a lot of memory and can cause
out of memory errors. 

* Ishmael spectrogram plug in graphics occasionally disappear when
detection parameters are adjusted. 

* The vertical scale on the raw data plug in display is incorrect. 

* Sound recorder. 1/10 of a second of data are occasionally lost when the
 recorder stops and restarts with a new file. 

**New Features in existing modules**

* Window state remembered on shut down and window restored to same size
 on start-up

* Show modules with no processes in viewer. This allows them to be renamed

* Better drawing of held spectrogram when putting mark rectangles on a
spectrogram display. All panels are now correctly frozen. The rectangle is
drawn in red on the marked panel and in green on other panels. 

* Spectrogram. Frequency information displayed in a &#8216;hover&#8217; box when the
mouse is moved over the display. Choice of colour maps for display. 

* Sound playback. Now supported in ASIO sound cards, so you can have sound
coming in through the ASIO card and back out through its headphone socket
(after passing through PAMGUARD filters and other modules) at the same time.

* Operation will depend on the configuration of individual sound cards and
how they are configured to mix incoming data with data from the PC. This is
sometimes a physical switch on the card and sometimes a software configuration
utility specific to that sound card. 

* NMEA/GPS Simulator has options that set the start location, the speed,
course, random course changes and whether or not AIS data are output 

* (AIS Data are a fixed data set recorded in the English Channel).

**Multithreading**

PAMGUARD now processes data in each module in a different
processing thread. This allows PAMGUARD to use both processor cores on dual or
multi core processor machines. However, multithreading may still cause
instability in some modules. An option is therefore accessible from the main
Detection menu to enable and disable multithreading. 

**New Modules**

The following new modules have been added to the Beta
Release 1.2.0

Sound Processing

1. Seismic Veto. A module for detecting and then cutting out
very loud sounds. 

Utilities

2. Depth Readout. Readout for analogue depth sensors using
MeasurementComputing data acquisition devices. 

Visual Methods

3. Video Range measurement. Will not capture video, but can
open jpeg, bitmap files, etc. or you can paste in an image for video range
analysis. 

4. Angle Measurement. Shaft angle encoding using Autonnic
Research Ltd A3030 Absolute Rotary Encoder (good for measuring the angle of binocular
stands).

5. Fixed Landmarks. Enter and display fixed points for
overlay on the map.  

## <a name="_Toc312065308"></a>Version 1.2.01 Beta. December 2008

**Bug fix** in whistle detector bearing calculation. 

1.3.00 Beta. January 2009

**New modules**

New Likelihood detector. 

The likelihood detector module is an implementation of a
likelihood ratio test with flexible algorithms and configuration to estimate
likelihood. It is 

suitable for detecting both short duration Odontocete clicks
(Sperm, Beaked, etc.) as well as moderate duration Mysticete calls (Humpback,
Bowhead, etc.).It is not suitable for detecting whistles.

Note that the likelihood detector will not run with PAMGUARD
in multithreading mode. 

**Bug fixes**

Click Detector - click train detector. Further
synchronisation to deal with multithreading. 

Depth Readout - Bug fix to prevent crash when dialog is
opened when no input device is installed.

Localiser &#8211; Further synchronisation of detection group
localiser (affects stability of click train localiser and whistle localiser in 

multithread operation). 

## <a name="_Toc312065309"></a>Version 1.3.01 Beta. January 27 2009

**Bug Fixes**

Fixed bug in serial port communication which caused crash is
a serial port 

didn&#8217;t exist. 

## <a name="_Toc312065310"></a>Version 1.3.02 Beta. February 2009

Beta release 1.3.02 is identical to Core release 1.3.00
apart from 

some modules which are known to be unstable or are new or
under development.

Modules which have only been included in the Beta release
are:

**Utilities Group**

Simulator (unstable)

Hydrophone Depth Readout (requires further testing and help
file)

Sound Processing Group

Patch Panel (unstable and requires help file)

Seismic Veto (requires help file and minor bug fixing)

**Visual Methods Group**

Angle Measurement (requires further testing and help file)

Video Range (requires help file)

Fixed Landmarks (requires help file) 

**Changes**

The User Input module (for text entry of information by the
user) has been moved from the Displays sub menu of the Add Modules menu, to
Utilities. This will not affect how existing settings files are loaded. 

**Multi-screen environments.**

In previous versions, if PAMGUARD was configured in a
multiscreen environment and is then run on a single screen, it was possible
that the display would not appear. PAMGUARD now checks screen dimensions at
start up and ensures that the main display is visible. Consequently, program
start-up takes slightly longer than previously. 

**Outstanding bugs**

The patch panel is known to crash if output channel numbers
from the patch panel a higher than the highest input channel number of the data
acquisition.  

**Bug fixes**

Speed up of graphics, particularly regarding large
quantities of gps track  data

National Instruments cards with names > 20 characters
long are now correctly  recognised. 

**Help File Updates**

The help viewer has been debugged and table of contents,
search and index features now all work as they should.

The layout of the help content has been re-ordered into more
functional groups (as laid out in the PAMGUARD add modules menu). 

Improved help has been added for the following modules:

* NMEA and GPS connections.

* Map Display

* Hydrophone Array Manager

* New help sections have been written for

* The model viewer

* Multithreading

* Map Overlays

* AIS

* User Input

* Airgun display

* Sound Recorder

* Spectrogram Smoothing

## <a name="_Toc312065311"></a>Version 1.3.03 Beta March 2009

Bug fix. ASIO sound cards reporting errors if a channel list
that is not 0,1,2 is used. This has been corrected so no false error messages
are sent. 

## <a name="_Toc312065312"></a>Version 1.4.00 Beta June 2009

**New Features**

Details of new features are documented in the PAMGUARD
online help. 

**PAMGUARD Viewer **

Reloads data from a PAMGUARD output database for viewing

**PAMGUARD Mixed Mode operation**

Analyses data from wav or AIF file and synchronises it with
GPS data reloaded from a database so that detected sounds may be correctly
localised. Multiple display frames - enables PAMGUARD GUI to be split into
multiple display windows, displayed on multiple monitors if desired. Enables
the operator to simultaneously view the map and the click detector for example,
which is much  easier than continually moving between multiple tabs. 

**Documentation**

The help file pages for the acquisition module have
undergone major revision Minor additions and edits have been made to many other
help pages

**Bug Fixes**

Memory leak caused by Night / Day colour manager now fixed.
This would cause memory leaks when multiple files were being analysed off-line
and would eventually crash PAMGUARD. The way that colours are managed in
PAMGUARD has been rewritten.  

Some issues with Swing fixed that might have been causing
hang ups on startup especially on Macs/Linux machines

 Some tidying up of events and default ports for serial
devices to improve GPS data collection (particularly for USB-Serial GPS on
Mac/Linux machines)

**Click Detector**

Parameters dialog shows correct channel numbers

Click classifier mean filter option had a computational
error which meant that classifiers using this option would not work. This has
now been fixed. 

**Sound Recorder**

Level meters are shown for the correct channels (after
channel numbering has been changed). Occasional crash due to synchronisation
problems when  multi-threading now fixed.

**Whistle Detector**

Correctly triggers recordings when whistle events are
detected

**Sound Playback** through ASIO sound cards Will now work
with any selected channels (not just channels 0 and 1)

## <a name="_Toc312065313"></a>Version 1.5.00 Beta - NI Support

**Bug Fixes**

Decimator was calculating incorrect filter coefficients
(coefficients were based on the output sample rate, not the input sample rate,
so it's likely that actual filtering of data was minimal). This is now
rectified. Ishmael matched filter. Would crash if template file not correctly
loaded. Now issues a warning message to the terminal and does not crash. 

Spelling correction on user input form (Sumbit - Submit)

**New Features **

National Instruments DAQ support (Windows 32 bit JVM Only)

The National Instruments interface has now been
comprehensively rewritten and is now stable. One or more NI data acquisition
devices can be used to acquire high speed data on multiple channels. See the
PAMGUARD help file for details. 

**Performance Tests**

A new menu item in the Help menu provides access to some
system performance tests. These are still under development but have been
released to Beta at the earliest opportunity since the development team require
feedback of test results verses performance on a variety of machines before the
tests and the  corresponding help files can be finalised.

## <a name="_Toc312065314"></a>Version 1.5.01 Beta

Bug fix. Channel number checking for ASIO cards changed
during NI support function and will always throw an error in 1.5.00. This has
now been fixed. 

## <a name="_Toc312065315"></a>Version 1.6.00 Beta

Identical to Core release 1.6.00 apart from additional
modules:

* Patch panel - unstable and requires documentation

* Angle Measurement (for binocular stands) - requires documentation

* Fixed landmarks - requires documentation.

**Bug fixes** since release 1.5.01

* Channel lists in output data streams of Decimator and other modules
fixed, so that when channel numbers change, downstream modules configurations
get the correct list of available channels. 

* GPS data from the GPGGA string are now correctly unpacked (formerly,
there was a bug in how times were unpacked between 1200 and 2400 GMT).

* Fixed NMEA dialog problems when no COM ports detected.   

**New Modules / Features** (see help file for details)

* Aural monitoring form. Module for input of data on when you're listening
and what you hear. Fully user configurable list of species. See help file for
more details. 

* Simulated sound sources. New sound source simulator. Accessible from
within the main data acquisition dialog. The old simulator didn't work well and
has been deleted.

* Copying and Printing

* New menu functionality by right clicking on any of the tabs of the main
tab control will allow the user to copy the tab contents to the system
clipboard from where it can be copied into other programs (e.g. Word,
Powerpoint, etc.).Some modules, such as the map, have this implemented in other
menus (right click) and also allow printing.  

* Installer now checks for previous versions of PAMGUARD and uninstalls
them before proceeding with installation.

* Installer now setting permissions on some of the default settings files
so that under Vista they can still be written to without needed to be an
Administrator

* When running the *.exe launchers on a 64 bit version of Windows they now
perform a check as to whether a 32bit JVM is available. (Until 64bit versions
of all the relevant shared libraries are available a 32bit JVM is still
required to get access to the full functionality of PAMGUARD)

## <a name="_Toc312065316"></a>Version 1.7.00 Beta October 2009

**Bug fix**. Spectrogram time and frequency scales now
display correctly and amplitude scale updates after scale parameter changes. 

**Speed improvement**. 

PAMGAURD FFT calculations now use the JTransforms FFT
library which gives a factor 2 speed improvement compared to previous FFT
methods used in PAMGUARD

**New Modules**

Two new modules are packaged. These are still in the final
stages of development. Feedback is most welcome. See online help for further details.

Whistle and Moan Detector

Whistle Classifier. 

## <a name="_Toc312065317"></a>Version 1.7.02 Beta November 2009

Bug Fixes. 

Airgun display occasionally crashing with a class cast
exception is fixed.

FFT modules, new code in 1.7.00 for interleaving FFT blocks
from multiple channels now no longer crashes.  

## <a name="_Toc312065318"></a>Version 1.8.00 Beta January 2010

**New features**

* Major changes to the multi-threading model in PAMGUARD. 

* This results in a major speed up of data exchange between modules and can
lead to a x4 improvement in overall performance. 

* Additional speed improvements have been made to the click detector which
results in an approximate 30% speed improvement for that one module. 

* Sound playback when monitoring using a sound card (previously only
possible with ASIO sound cards) but see warning in help file about sound card
synchronisation.

* Wigner time-frequency plot for the click detector. Clicks are NOT
automatically displayed in the Winger window as they are for the waveform and
spectrum windows since the Wigner transformation is very time consuming. Clicks
will be displayed if you select them with the mouse.

* New click classifier methods, which include extraction of frequency
sweep parameters. (Please note that this click is not yet documented. The old
classifier is still available). 

* Bearing ambiguity resolution for planar and volumetric arrays. New
features in array dialog for estimations of errors on array location. These
feed into to a maximum likelihood estimate of angles from small 2D or 3D sub
arrays to give two polar angle coordinates. this can be used to resolve left
right ambiguity and is implemented for both the click and the whistle/moan
detectors. As a result, the click detector bearing time display can now be set
to go from -180 to 180 degrees rather than 0 - 180 degrees. In this case,
clicks in the upper half of the bearing display will be to port and clicks in
the right half will be to starboard. As you pass a whale which is to port, the
clicks will move UP the display. In the long term, I hope to turn this display
around so that time is up the screen rather than across.  

**Small features and bug fixes**

* Checks on Recordings and Clicks output folders before PAMGUARD starts. 

* Colours in spectrogram amplitude display correctly match the user
selection.

* Default parameters in the radar display are for quiet clicks to be shown
further away. 

* Ishmael spectrogram correlation module checks for kernel creation and no
longer crashes if the kernel is not present. 

## <a name="_Toc312065319"></a></a>Version 1.8.01 Beta February 2010

**New features**

* Play back of high frequency wav files through National Instruments (NI) cards:
When analysing wav files, sound can be output at high frequency (depending on
the specification of the NI card). The primary motivation for this has been to
develop a test bed for high frequency sound acquisition whereby we can play
500kHz recordings of harbour porpoise out of an NI card on one machine, into NI
cards on a different machine which are testing high frequency versions of the
click detector.

* Note that this does not (yet) permit simultaneous acquisition and
playback through

* NI data acquisition cards. 

**Minor features and bug fixes**

* Synchronisation of FFT functions. Prevents threads accessing the same
FFT module simultaneously which was occasionally causing crashes of the click
detector if multiple hydrophone groups were being used. 

* Pop up menus on click detector displays: some of these were not
displaying correctly when running under Linux. This has been fixed.  

**Bug fixes**

* patch panel can now output channels which are not in it's input list.

* Null pointer trapped in Ishmael spectrogram correlation module. 

* False buffer overflows at low sample rates stopped (size of data in
individual data blocks had been increased at low sample rates, but when this
exceeded 3s a false buffer overflow would occur).

## <a name="_Toc312065320"></a>Version 1.8.01 Beta February 2010

**Bug Fixes**

Crashes caused in real time or Mixed Mode if the number of
software channels exceeds the number of hydrophones configured in the array
manager. It is of course illogical to have software channels which are not
assigned to a hydrophone, so rather than entirely prevent PAMGUARD from
crashing I have inserted additional checks into the hydrophone array dialog
which will prevent the user from closing that dialog unless all software
channels are assigned to a hydrophone. 

## <a name="_Toc312065321"></a>Version 1.9.00 Beta April 2010

**Channel numbering**

A new internal channel numbering scheme for ASIO sound cards
was released on 8 March in Alpha version 1.8.02. These changes have now been promoted
to this Beta release 1.9.00. These changes were described in an email to
PAMGUARD User, the text of which is repeated here:

Within PAMGUARD, there have been constant problems with
lookup tables relating hardware channel numbers to internal channel numbers.
99.9% of the time, this is not a problem since people are just reading channels
0 and 1 from a normal sound card or a list of channels 0,1,2,3, etc from an
ASIO card or National Instruments card. However, when using the ASIO sound
acquisition system, it is possible to read out hardware channels 2,3,4,5 say
and those numbers would propagate through PAMGUARD and be used by all the
detectors. While this gave some clarity to  the displays, it also created
a number of problems since look up tables were constantly required to relate
hardware numbers and software numbers in every PAMGUARD module. Since this type
of channel numbering was only implemented several years into the PAMGUARD
project, it was never fully implemented or well supported by the different
modules which resulted in a number of bugs which could cause confusion as to
which hydrophones were being used during localisation or during calibrated
measurement. The situation tended to only arise with a small number of ASIO
sound cards such as the RME Fireface 400 on which the most useful inputs, the
balanced line inputs, are hardware channels 4,5,6 and 7 on the back of the
instrument. (On the Fireface 800, the balanced line inputs are channels 0 to
7). When using the National Instruments system, data were always sent into the
rest of PAMGUARD with sequential channel numbering starting at 0. This was
required in order to support multiple NI Daq boards where it is possible to
read for example channel 0 and 1 on two different devices, so to uniquely identify
channels in the rest of PAMGUARD, the only rational thing to do was to re-label
those channels 0,1,2,3. 

There have been other annoyances with the ASIO channel
numbering scheme. For instance, if you had a configuration which worked in real
time using channels 7 and 8 of a sound card, you could not use that same
configuration to analyse wav files, which would always read their data as
channels 0 and 1. Similarly if you switched sound cards, you may have to
reconfigure every detector and several of the displays to handle the changes in
channel numbering. 

I have spent a considerable amount of time trying to work
out a better system for handling channel numbering in PAMGUARD and have decided
that the only practical thing to do is to force all software channel numbering
back to a zero indexed system. i.e. even if you read out hardware channels
3,4,5 and 6, within PAMGUARD, everywhere apart from the Sound acquisition
dialog, you will see them listed as channels 0,1,2 and 3.  The
consequences of doing this are mainly very good. Most users will not notice any
changes. For those who need for some reason to read channel numbers not
starting at zero things will be more stable. However, when you go to a new
version with this new feature, it will be necessary to reconfigure all the
modules in any existing configurations. To do this, open the configuration
dialog for each module in turn and select the correct, zero indexed, channel
number. 

**Other Changes / new features**

PAMGUARD Viewer

The way in which data are loaded for each module has changed
in the PAMGUARD viewer. In the scroll bar for each display, at the right hand
end, there are buttons for pagingforward, backward and setting page length
options. 

Click Detector Bearing Time display

Older versions used a vertical scroller in the right hand
side of the main bearing time display to control the display length. This has
been removed and replaced by a small control consisting of an edit box and a
spinner (up and down arrows) at the right hand end of the horizontal scroll
bar. The scroll system now also enabled you to scroll back through up to one
minute of data during real time operation. 

Whistle and Moan Detector

Bearings are now written to the database.

PAMGUARD Viewer scrolling mechanism has changed. 

Each display now has a small control in the right hand end
of a scroll bar or a slider which allows you to page through data. This is only
well implemented for the map, but will be an increasingly important feature as
more displays are set up for offline data viewing in future releases. 

Viewer data map

When using the PAMGUARD viewer an additional panel will be
displayed showing an overview of data quantity (per hour) from the various
database tables connected to PAMGUARD. 

Spectrogram Display overlays

The mechanism behind this has changed. Should not affect
operations, but if you have problems with overlays on the spectrogram, please
notify [support@pamguard.org](mailto:support@pamguard.org) 

All display overlays

Now pick up line colours from the Display symbol manager, so
it's possible to set line colours by selecting a symbol with a particular line
colour from the Display menu. 

Database

Default name for the Database module has changed from
"ODBC Database" to just "Database". This will not affect
existing configurations. 

Sound acquisition

Changes made for a previous Mac build now incorporated into
main Beta branch. This should allow for basic 2 channel sound acquisition using
Macs. 

Bug Fixes

Whistle and Moan Detector 

Was not detecting sounds when noise reduction was performed
in the Whistle Moan Detector, but was OK when noise reduction ran in the FFT
module. This problem has been resolved.

Whistle Classifier

Bug fix in database output. The first species probability column
was setting to zero. This is now fixed

Ishmael Detectors

Better rendering of detection boxes on spectrogram display
and more meaningful names in spectrogram display menu.

Sound acquisition

When not using NI or AISO cards PAMAGUARD acquires sound
using javax.sound which only supports two channels. Max number of channels now
specified as 2 rather than undefined.

Serial Comms

A few changes so more through tidying up of resources when a
port is closed. Might prevent a few problems when using a GPS. 

## <a name="_Toc312065322"></a>Version 1.9.01 Beta May 5 2010

**Bug fixes**

Acquisition dialog for ASIO sound cards was resetting all
channel numbers to zero.

Degree symbols throughout PAMGUARD have been fixed so that
they work on all Win64, (Mac and Linux.)

An unnecessary channel warning from the seismic veto has
been removed. 

Contour check boxes on the map dialog have now been placed
in a scrolling pane so that if there are lots of contours, the dialog doesn't
become too bit to fit on a screen. 

**New Feature**

psf file name is now displayed in the title bar of the main
PAMGUARD display. 

Windows Installer now sets permissions on PAMGUARD folder so
that don't need to be an administrator in order to create new files in that
directory. Prevents problems especially under Vista/Windows 7 when saving psfs
and recording sounds to the default directory.  

## <a name="_Toc312065323"></a>Version 1.10.00 Beta December 2010

**New features**

1. Binary storage module. 

Performs a parallel role to the PAMGAURD database but uses
binary files in a proprietary format which is considerably more efficient for
data of unknown length such as whistle contours or small clips of click
waveform. This feature is currently implemented in the following modules:

* Click Detector

* Whistle and Moan detector

* AIS 

2. Improved offline viewer functionality. 

The offline viewer is much improved with better data
scrolling and the ability to view data either stored in the database or the
binary storage system. Work has stated on functions which read and use data
stored in audio files. This is currently limited to re-calculating and
displaying displaying spectrogram data during viewer mode operation.  

3. Heading sensor readout

PAMGUARD can now read true or magnetic heading data from
gyro compasses, fluxgate compasses, etc. See help for details.

4. Click detector offline event marking

Functionality for offline analysis, similar to that in
RainbowClick is now partially incorporated into the PAMGUARD click detector in
viewer mode. Help files have not yet been developed for these features. 

5. Better support in the Decimator module for non-integer
frequency division (i.e. previous versions would convert well from 96kHz to
48kHz, but not from 500kHz to 48kHz) 

**Bug fixes**

1. Placement of axes on some displays (array manager and
depth side panel). Problem in 1.9.01 Beta now fixed. 

2. Fixed problems of NMEA read out not restarting after a
serial drop out. NMEA now restarts correctly. 

3. Fixed a memory leak which caused occasional crashes,
particularly when large numbers of files were processed offline. 

4. The correct file path separator is used in Sound
recording names under Linux (which uses "/" as opposed to
"\" on Windows).

## <a name="_Toc312065324"></a>Version 1.11.01 Beta April 2012

**New Modules**

* ROCCA Whistle Classifier (see online help)

* Noise Measurement (see online help) 

* Logger Forms

**Click Detector Updates**

* Amplitude Selector. This is a small popup window which allows you to set
a minimum amplitude for clicks to be displayed on the bearing time display.
Primarily useful during offline analysis using the viewer to remove clutter
from the display. 

* ICI of unassigned clicks. Previously ICI was only shown for clicks
assigned to a click train. Can now be shown for all clicks, but note that this
will be meaningless if there is > 1 animal present. 

* Echo detection. Automatic detection of echoes (see online help). 

* Offline event marking. Offline event marking similar to functionality in
RainbowClick is now available in the PAMGUARD viewer. Event summary data is
stored in the database and can also be exported to text files (e.g. for
importing into the Distance software).  

* Target motion analysis in two and three dimensions using a variety of
models to fit the data (see help files for details).

* Toolbar with display species selection.

* Bearings can be calculated using the envelope of the waveform rather
than the full waveform. The waveform or envelope can also be filtered prior to
bearing calculation.

* Click species templates. Spectral templates may be generated / reloaded
for different types of click.

* Click concatenated spectrogram display helps to view spectral properties
of multiple clicks in an event.

**Raw Data Loading**

In viewer mode, use the Acquisition control dialog to select
the location of wav files associated with the data. This will unlock two useful
features of the display:

* The spectrogram will automatically generate and display spectrogram
data. 

* The spectrogram display and the click detector will both allow you to
play back sound via the system sound card. 

**Minor Features and Bug Fixes**

* The default preamplifier gain in the acquisition module has been changed
to 0dN

* The Binary store now saves a complete data map between runs of the
PAMGUARD viewer. This leads to significant improvements in the time the viewer
takes to load large data sets.

* DataMap: The click and the  whistle and moan detectors can now also
show graphs of data density as a function of frequency instead of simple bar
charts of data density vs. time (datagrams). This makes it much easier to find
features such encounters with animals. 

* Formatting of times in scroll bars has been improved (e.g. will say 15 m
instead of 900s)

* The FFT Engine can now copy with FFT hops which are greater than the FFT
length. 

* The filter module can now also generate finite Impulse Response (FIR)
filters. Note however, that FIR filters may require considerably more execution
time than IIR filters. 

* Database support for MS Access 2007 and 2010 database formats. 

* 3D bearings from click detector using volumetric arrays. For small
arrays (where the distance between elements is less than a few hundred samples)
bearings are now correctly calculated in three dimensions. 

* Main GUI frame no longer disappears behind other windows when viewer
data are loaded. 

* Toolbar with quick access to start / stop buttons. 

* Filter and Decimator modules output data name. These were not unique in
previous versions which made it impossible for downstream modules to connect to
the correct data stream. This has been fixed. 

* Whistle classifier has some new features in the classifier training
panel. It is also now possible to export training data files directly from
binary data files. 

* The hydrophone localisation systems have been updated to better use
information from true and magnetic heading sensors.

* The FFT module has been updated to ensure it finds it&#8217;s correct data
source, even if that data source is loaded after the FFT module is created.

## <a name="_Version_1.11.02_Beta"></a>Version 1.11.02 Beta May 2012

**New Features**

* Hiding side panels. The panel on the left of the display can be hidden &#8211;
this creates more space for other displays. 

* Click Detector Displays. Numbers of small displays (e.g. the Wigner
plot) is now remembered between runs. 

**Bug Fixes** 

* PAMGuard Colours: Now selects correct night time colours at programme
start-up.

* Bug in Noise Monitor. Version 1.11.01 would crash if only a subset of
channels were selected, e.g. if you only measured noise on channel 0 of a two
channel configuration. This bug is now fixed

## <a name="_Version_1.12.00_Beta"></a>Version 1.12.00 Beta February 2013

**New Modules**

Clip Generator: Saves and
displays short waveform clips around detections. Can work with any detector (or
indeed any other data stream within PAMGuard).  

Local time display: displays the
computers local time on the main display panel. 

WILD interface: provides the user
the ability to integrate Pamguard with the WILD ArcGIS-based marine mammal
survey software package.

**New Features**

_Core Functionality_

New storage options have been
implemented which give the user greater control of where data are stored. 

Modules have been arranged into
different groups in the configuration menus and tool tip texts have been added
to these menus to provide additional information to users.  

_Radar Display_

Functionality has been added to
the radar display so that bearings can be shown relative to either the vessel
or to true North. 

Better control of data in viewer
mode, making is easy to scroll through and view data for short time periods. 

_GPS_

Function to import GPS data from
other data sources for the PAMGuard viewer. 

_Database_

Can now copy data from binary
storage to the database offline for any module having both binary and database
storage. 

Can create a blank MS Access
database (2007 and later *.accdb formats only).

Can open MS Access from within
PAMGuard to make it easier to view database content. 

Support for open office databases.

_Sound Acquisition_

National Instruments cards: Added
code in support of the new x-series devices. 

ASIO sound cards: Added support
for a new open source ASIO driver system (jasiohost). The old system has been
left in place for now while we assess users response to the newer system. 

_AIS_

Support has been added to the AIS
module to read data from class B AIS stations (used by smaller vessels), Base
stations and Aids To Navigation. 

_Spectrogram Display_

Can now scroll as well as wrap
the data. 

_Whistle Detector_

Stores amplitude and bearing
information correctly in the binary files. 

_Logger Forms_

A substantial amount of work has
been carried out on Logger forms, funded by the South West fisheries Science
Center. 

_Click Detector_

Two additional displays have been
added to the click detector

1. Concatenated
Spectrogram which enables users to view spectra for multiple clicks within the
same event.

2. Inter
Detection Interval Display which provides a visual interpretation of the
inter-detection interval.

Target Motion
Analysis: Updated target motion analysis module so that it works in three (as
opposed to two) dimensions.

Alarm in click
detector to issue audible warning when certain click types are detected. 

Display options have
been improved making it easier to display only certain types of clicks. 

_Ishmael
Detection Modules_

We have
implemented database storage for output of these modules. 

**Bug fixes**

* Radar Display: A bug which stopped the radar display from
correctly displaying bearings to whistles from arrays containing more than two
hydrophone elements has been fixed. 

* Database Speed: A substantial rewriting of some of the indexing
methods in the database module has led to a significant increase in the speed
at which data are written to the database (orders of magnitude for large
databases). This is having a significant impact on the overall reliability of
the software. Other changes have increased the speed (again by orders of
magnitude) at which data are read back into PAMGuard when using the viewer. 

* PAMGuard start-up options have been substantially improved:

1. Pressing
cancel will now exit PAMGuard

2. Clearer
when a  new (blank) configuration is being created

3. Can
create a database for viewer mode from scratch. 

* Timing calculations for simulated data have been improved. 

