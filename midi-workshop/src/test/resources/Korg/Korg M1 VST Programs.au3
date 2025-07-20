; -----------------------------------------------------------------------------------------------------
; This AutoIt3 script retrieve automatically Program names and categories from Korg M1 VST
; It relies on free OCR Tesseract so you need to install it somewhere
; Most of it has been made using Google Gemini in less than 4 hours without knowing anything about it
; -----------------------------------------------------------------------------------------------------

#Region
Opt("SendKeyDelay", 10)
Opt("SendAttachMode", 1)
Opt("MouseCoordMode", 0) ; Mouse coordinates RELATIVE TO THE ACTIVE WINDOW (for MouseMove/Click)
Opt("PixelCoordMode", 0) ; Pixel coordinates RELATIVE TO THE ACTIVE WINDOW (for PixelGetColor)
#EndRegion

#include <ScreenCapture.au3>

; #####################################################################
; ####################### PARAMETERS TO MODIFY ########################
; #####################################################################

; Exact title of the target window for all grids
Global $sWindowTitle = "M1"

; --- CARDS grid (7x3) Parameters ---
Global $iCards_StartX = 460
Global $iCards_StartY = 295
Global $iCards_ButtonWidth = 77
Global $iCards_ButtonHeight = 24
Global $iCards_NumColumns = 7
Global $iCards_NumRows = 4

; --- PROGRAMS grid (5x10) Parameters ---
Global $iPrograms_StartX = 250
Global $iPrograms_StartY = 408
Global $iPrograms_ButtonWidth = 192
Global $iPrograms_ButtonHeight = 25
Global $iPrograms_NumColumns = 5
Global $iPrograms_NumRows = 10
; Numbering offset for Programs after scrolling
Global $iPrograms_TotalOffset = $iPrograms_NumColumns * $iPrograms_NumRows

; --- CATEGORIES grid (4x4) Parameters for color detection ---
Global $iCategories_StartX = 449
Global $iCategories_StartY = 296
Global $iCategories_CellWidth = 102
Global $iCategories_CellHeight = 25
Global $iCategories_NumColumns = 4
Global $iCategories_NumRows = 4
Global $iCategories_ActiveColor = 0xFFFFFF
Global $iCategories_ColorTolerance = 0

; --- Misc
Global $iDelayBetweenClicks = 250
Global $iDelayAfterCardsCycle = 250 ; Example: 0.5 second
Global $iMouseSpeed = 1

Global $sLogFileName = "Korg M1 VST Programs.txt"
Global $hFileLog

; --- Tesseract OCR Parameters ---
Global $sTesseractPath = "C:\Tools\Tesseract-OCR\tesseract.exe"
Global $sTesseractLang = "eng"
Global $sTempImageFile = @ScriptDir & "\ocr_temp.png"
Global $sTempTextFile = @ScriptDir & "\ocr_temp.txt"
Global $iProgramName_OCR_X = 244
Global $iProgramName_OCR_Y = 332
Global $iProgramName_OCR_Width = 189
Global $iProgramName_OCR_Height = 36

; #####################################################################
; ####################### AUTOIT SCRIPT START #########################
; #####################################################################

; Set an escape key to stop the script
HotKeySet("{ESC}", "StopScript")

; Wait for the window to exist and make it active
Local $hWnd = WinWaitActive($sWindowTitle, "", 10)
If Not $hWnd Then
    MsgBox(16, "Error", "The window '" & $sWindowTitle & "' was not found or did not become active after 10 seconds.")
    Exit
EndIf

; Open the log file in write mode (creates the file or overwrites if it exists)
$hFileLog = FileOpen($sLogFileName, 2) ; Mode 2 = write (overwrite if exists)
If $hFileLog = -1 Then
    MsgBox(16, "File Error", "Cannot open log file: " & $sLogFileName)
    Exit
EndIf

ClickOn_PROG()
ClickOn_BROWSER()
ScanCards()

FileClose($hFileLog) ; Close the file at the end of the script

MsgBox(64, "Completed", "The script has finished. Logs are saved in " & $sLogFileName & ".")
Exit ;

; #####################################################################
; ####################### ADDITIONAL FUNCTIONS ########################
; #####################################################################

Func ScanCards()
	; Main loop for the CARDS grid
	Local $prevCardTitle = ""

	For $iCardsRow = 0 To $iCards_NumRows - 1
		For $iCardsColumn = 0 To $iCards_NumColumns - 1
			Local $iCardsCase = ($iCardsRow * $iCards_NumColumns) + $iCardsColumn ; Cards case number (0-based)
			if ($iCardsCase == 22) Then
				Return
			EndIf

			ClickOn_CARD_Tab()

			; Click on a button in the CARDS grid
			Local $iPosX_Cards = $iCards_StartX + ($iCardsColumn * $iCards_ButtonWidth) + ($iCards_ButtonWidth / 2)
			Local $iPosY_Cards = $iCards_StartY + ($iCardsRow * $iCards_ButtonHeight) + ($iCards_ButtonHeight / 2)

			MouseMove($iPosX_Cards, $iPosY_Cards, $iMouseSpeed)
			MouseClick("left")
			Sleep(1000)

			; Use PerformOCR for cardTitle
			; Parameters are X, Y, Width, Height
			Local $cardTitle = PerformOCR(976, 268, 153, 108)
			ConsoleWrite("Card title: " & $cardTitle & @CRLF)
			if ($cardTitle == $prevCardTitle) Then
				MsgBox(48, "Script Stop", "Card is unchanged: " & $cardTitle)
				Return
			EndIf
			$prevCardTitle = $cardTitle

            ; First chunk of 50 programs
            ; Scrollbar on the left
			ScanProgramsGrid($iCardsCase, $cardTitle, 0)

			; Optional second chunk of 50 programs
			; Scrollbar forced to the right
			ClickOn_CARD_Tab()
			Sleep(250)
			; Check if the scrollbar can be moved to the right
			Local $iPixelColor = PixelGetColor(1133,668)
			If _CompareColors($iPixelColor, 0x000000, 0) Then
				ScanProgramsGrid($iCardsCase, $cardTitle, 1)
			EndIf

			; Delay after the PROGRAMS grid has been fully scanned for a CARDS grid button
			Sleep($iDelayAfterCardsCycle)
		Next
	Next
EndFunc

; Function to scan the PROGRAMS grid with scroll and offset management
; $iCurrentCardsCase : Number of the clicked case in the Cards grid
; $sCardTitle : The title of the currently processed card
; $iScrollBarIndex : 0 for the first segment (no scroll/offset), 1 for the second segment (with scroll/offset)
Func ScanProgramsGrid($iCurrentCardsCase, $sCardTitle, $iScrollBarIndex)
    Local $iProgramsOffset = 0
    Local $sLogPrefix = ""
    Local $iClickX1 = 0
	Local $iClickX2 = 0
    Local $iClickY = 672 ; Y is common for both scroll clicks

    If $iScrollBarIndex = 0 Then
        ; No offset for the first segment
        $iProgramsOffset = 0
        $sLogPrefix = "  "
        $iClickX1 = 290
		$iClickX2 = 290
    ElseIf $iScrollBarIndex = 1 Then
        ; Apply offset for the second segment
        $iProgramsOffset = $iPrograms_TotalOffset
        $sLogPrefix = "    "
		$iClickX1 = 290
        $iClickX2 = 1200 ; New X coordinate for ScrollBarIndex = 1
    Else
        ConsoleWrite("ERROR: ScanProgramsGrid called with invalid ScrollBarIndex: " & $iScrollBarIndex & @CRLF)
        Return
    EndIf

    For $iProgramsColumn = 0 To $iPrograms_NumColumns - 1
        For $iProgramsRow = 0 To $iPrograms_NumRows - 1
            ClickOn_CARD_Tab()

			; Force the scroll bar in the right or left position
            MouseMove($iClickX1, $iClickY, $iMouseSpeed)
            MouseClick("left")
            MouseMove($iClickX2, $iClickY, $iMouseSpeed)
            MouseClick("left")

            ; Programs case number (0-based) - Formula adjusted for "column by column" numbering
            ; Add offset based on $iScrollBarIndex
            Local $iProgramsCase = ($iProgramsColumn * $iPrograms_NumRows) + $iProgramsRow + $iProgramsOffset

            ; Calculate the top-left position for OCR detection of the program name within the grid cell
            Local $x = $iPrograms_StartX + ($iProgramsColumn * $iPrograms_ButtonWidth)
            Local $y = $iPrograms_StartY + ($iProgramsRow * $iPrograms_ButtonHeight)

            ; Perform OCR to get the text of the program case BEFORE clicking
            Local $programCaseText = PerformOCR($x, $y, $iPrograms_ButtonWidth, $iPrograms_ButtonHeight)

            ; Check if the case text is empty (or contains only whitespace after stripping)
            If StringStripWS($programCaseText, 3) = "" Then
                ConsoleWrite($sLogPrefix & "  Program case " & $iProgramsCase & " is empty. End of card's programs" & @CRLF)
                Return
            EndIf

            ; Calculate the center position of the Program name for clicking
            Local $iPosX_Programs = $x + ($iPrograms_ButtonWidth / 2)
            Local $iPosY_Programs = $y + ($iPrograms_ButtonHeight / 2)

            MouseMove($iPosX_Programs, $iPosY_Programs, $iMouseSpeed)
            MouseClick("left")
            Sleep($iDelayBetweenClicks)

            ; Perform OCR to get the programName
            Local $programName = PerformOCR($iProgramName_OCR_X, $iProgramName_OCR_Y, $iProgramName_OCR_Width, $iProgramName_OCR_Height)
            If $programName == "" Then
                ConsoleWrite($sLogPrefix & "  Program Name OCR: <Not Detected>" & @CRLF)
            EndIf

            DetectActiveCategoriesCase($iCurrentCardsCase, $sCardTitle, $iProgramsCase, $programCaseText, $programName) ; Pass programName to the logging function

        Next
    Next
EndFunc
Func ClickOn_SEARCH_Tab()
	MouseMove(485, 250, $iMouseSpeed)
	MouseClick("left")
	Sleep(500)
EndFunc
Func ClickOn_CARD_Tab()
	MouseMove(585, 250, $iMouseSpeed)
	MouseClick("left")
	Sleep(250)
EndFunc
Func ClickOn_PROG()
	MouseMove(925, 114, $iMouseSpeed)
	MouseClick("left")
	Sleep(250)
EndFunc
Func ClickOn_BROWSER()
	MouseMove(642, 114, $iMouseSpeed)
	MouseClick("left")
	Sleep(250)
EndFunc

; Function to detect the active case in the CATEGORIES grid
; $iCurrentCardsCase : Number of the clicked case in the Cards grid
; $sCardTitle : The card title
; $iCurrentProgramsCase : Number of the clicked case in the Programs grid
; $sProgramCaseText : The text of the program case (what Tesseract saw on the button)
; $sProgramName : Program name retrieved by OCR after the click
Func DetectActiveCategoriesCase($iCurrentCardsCase, $sCardTitle, $iCurrentProgramsCase, $sProgramCaseText, $sProgramName)
    Local $iFoundCategoriesCase = -1 ; Initialize to -1 (no case found)
    Local $iCategoriesCaseCounter = 0 ; For 0 to N-1 numbering

    ConsoleWrite("    --- CATEGORIES grid scan in progress (color detection) ---" & @CRLF)

	ClickOn_SEARCH_Tab()

    For $iRow = 0 To $iCategories_NumRows - 1
        For $iColumn = 0 To $iCategories_NumColumns - 1
            ; Categories case number (0-based)
            $iCategoriesCaseCounter = ($iRow * $iCategories_NumColumns) + $iColumn

            ; Calculate the top-left position of the cell (relative to the window)
            Local $iPosX = $iCategories_StartX + ($iColumn * $iCategories_CellWidth)
            Local $iPosY = $iCategories_StartY + ($iRow * $iCategories_CellHeight)

            ; Get the pixel color at this position RELATIVE to the active window
            Local $iPixelColor = PixelGetColor($iPosX, $iPosY)

            ; Compare the pixel color with the expected color (white) with tolerance
            If _CompareColors($iPixelColor, $iCategories_ActiveColor, $iCategories_ColorTolerance) Then
                $iFoundCategoriesCase = $iCategoriesCaseCounter
                ExitLoop (2) ; Exit both For loops (Row and Column) as soon as a case is found
            EndIf
        Next
    Next

    Local $msg = $iCurrentCardsCase & "|" & $sCardTitle & "|" & $iCurrentProgramsCase & "|" & $iFoundCategoriesCase & "|" & $sProgramName & @CRLF
    ConsoleWrite($msg)
    FileWrite($hFileLog, $msg)
EndFunc

; Helper function to compare two colors with a tolerance
Func _CompareColors($iColor1, $iColor2, $iTolerance)
    Local $iR1 = BitAND($iColor1, 0xFF)
    Local $iG1 = BitAND(BitShift($iColor1, 8), 0xFF)
    Local $iB1 = BitAND(BitShift($iColor1, 16), 0xFF)

    Local $iR2 = BitAND($iColor2, 0xFF)
    Local $iG2 = BitAND(BitShift($iColor2, 8), 0xFF)
    Local $iB2 = BitAND(BitShift($iColor2, 16), 0xFF)

    If Abs($iR1 - $iR2) <= $iTolerance And _
       Abs($iG1 - $iG2) <= $iTolerance And _
       Abs($iB1 - $iB2) <= $iTolerance Then
        Return True
    Else
        Return False
    EndIf
EndFunc

Func StopScript()
    MsgBox(48, "Script Stop", "The script has been interrupted by the user.")
    If IsInt($hFileLog) And $hFileLog <> -1 Then
        FileWrite($hFileLog, "--- Script interrupted by user ---" & @CRLF)
        FileClose($hFileLog)
    EndIf

    Exit
EndFunc

; Function to perform OCR on a screen region
; $iX, $iY : X, Y coordinates of the top-left corner of the region to capture (RELATIVE TO THE TARGET WINDOW)
; $iWidth, $iHeight : Width and height of the region to capture
; Returns the recognized text or an empty string in case of error
Func PerformOCR($iX, $iY, $iWidth, $iHeight)
    Local $sResult = ""
    Local $aLines[1] ; Dynamic array to store lines
    Local $iLineCount = 0

    ; Delete temporary files before processing
    FileDelete($sTempImageFile)
    FileDelete($sTempTextFile)

; Calculate bottom-right coordinates for _ScreenCapture_CaptureWnd
    Local $iX2 = $iX + $iWidth
    Local $iY2 = $iY + $iHeight

    Local $bSuccess = _ScreenCapture_CaptureWnd($sTempImageFile, $hWnd, $iX, $iY, $iX2, $iY2)

    If Not $bSuccess Then
        ConsoleWrite("SCREEN CAPTURE ERROR: Unable to capture window region with _ScreenCapture_CaptureWnd. Error: " & @error & @CRLF)
        Return ""
    EndIf

    ; Execute Tesseract via command line
    ; Tesseract path MUST be enclosed in double quotes if it contains spaces
    Local $sCmd = '"' & $sTesseractPath & '" "' & $sTempImageFile & '" "' & StringTrimRight($sTempTextFile, 4) & '" -l ' & $sTesseractLang & ' --psm 3' ; PSM 3 is good for general multi-line text

    Local $iPid = Run($sCmd, @ScriptDir, @SW_HIDE, $STDOUT_CHILD + $STDERR_CHILD)
    ProcessWaitClose($iPid)

    ; Read the result from the text file generated by Tesseract
    If FileExists($sTempTextFile) Then
        Local $hFileRead = FileOpen($sTempTextFile, 0) ; Mode 0 = read
        If $hFileRead <> -1 Then
            Local $sLine
            While 1
                $sLine = FileReadLine($hFileRead)
                If @error = -1 Then ExitLoop ; End of file
                ; Clean up leading/trailing whitespace from the line
                $sLine = StringStripWS($sLine, 3) ; 3 = remove leading and trailing whitespace

                ; Add non-empty line to the array
                If $sLine <> "" Then
                    ReDim $aLines[$iLineCount + 1];
                    $aLines[$iLineCount] = $sLine
                    $iLineCount += 1
                EndIf
            WEnd
            FileClose($hFileRead)

            ; Concatenate lines with the "/" character
            If $iLineCount > 0 Then
                $sResult = $aLines[0]
                For $i = 1 To $iLineCount - 1
                    $sResult &= "/" & $aLines[$i]
                Next
            Else
                $sResult = "" ; No lines recognized
            EndIf

        Else
            ConsoleWrite("OCR ERROR: Unable to open Tesseract output file for reading." & @CRLF)
        EndIf
    Else
        ConsoleWrite("OCR ERROR: Tesseract output file not found." & @CRLF)
    EndIf

    Return $sResult
EndFunc