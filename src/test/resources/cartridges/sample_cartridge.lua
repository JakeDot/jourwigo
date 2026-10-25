-- Sample Wherigo Cartridge Lua Source File for Integration Testing
-- Tests all implemented types and functions exposed to Lua

cartSampleCartridge = Wherigo.ZCartridge()

cartSampleCartridge.Name = "Integration Test Cartridge"
cartSampleCartridge.Description = "A sample cartridge using all Lua-exposed types."
cartSampleCartridge.Visible = true
cartSampleCartridge.Author = "Jourwigo"
cartSampleCartridge.Version = "1.0.0"

-- 1. ZonePoint
zp1 = Wherigo.ZonePoint(51.5074, -0.1278, 10.0)
zp2 = Wherigo.ZonePoint(51.5080, -0.1280, 5.0)

-- 2. Distance
dist1 = Wherigo.Distance(500, "m")
distVal = dist1:GetValue("m")

-- 3. Zone
zoneMain = Wherigo.Zone(cartSampleCartridge)
zoneMain.Name = "Start Zone"
zoneMain.Description = "The starting zone."
zoneMain.Visible = true
zoneMain.Active = true
zoneMain.Points = {
    Wherigo.ZonePoint(51.5070, -0.1270, 0),
    Wherigo.ZonePoint(51.5080, -0.1270, 0),
    Wherigo.ZonePoint(51.5080, -0.1280, 0),
    Wherigo.ZonePoint(51.5070, -0.1280, 0)
}
zoneMain.OriginalPoint = zp1

-- 4. ZCharacter (Thing character)
npcGuide = Wherigo.ZCharacter(cartSampleCartridge)
npcGuide.Name = "Guide NPC"
npcGuide.Description = "A friendly guide character."
npcGuide.Gender = "Male"
npcGuide.Visible = true

-- 5. ZItem (Thing item)
itemKey = Wherigo.ZItem(cartSampleCartridge)
itemKey.Name = "Golden Key"
itemKey.Description = "A shiny golden key."
itemKey.Visible = true

-- 6. ZCommand (Action)
cmdExamine = Wherigo.ZCommand(cartSampleCartridge)
cmdExamine.Name = "Examine"
cmdExamine.Text = "Examine object"
cmdExamine.Enabled = true

-- 7. ZMedia (Media)
mediaInfo = Wherigo.ZMedia(cartSampleCartridge)
mediaInfo.Filename = "sample_audio.mp3"
mediaInfo.Type = "mp3"
mediaInfo.AltText = "Sample Info Media"

-- 8. ZInput (EventTable Input)
inputQuestion = Wherigo.ZInput(cartSampleCartridge)
inputQuestion.Name = "Passcode Prompt"
inputQuestion.Question = "Enter passcode:"
inputQuestion.InputType = "Text"

-- 9. ZTimer (Timer)
timerCountdown = Wherigo.ZTimer(cartSampleCartridge)
timerCountdown.Name = "Test Timer"
timerCountdown.Type = "Countdown"
timerCountdown.Duration = 5

-- 10. ZTask (Task)
taskQuest = Wherigo.ZTask(cartSampleCartridge)
taskQuest.Name = "Complete Integration Test"
taskQuest.Description = "Run all exposed Lua methods."
taskQuest.Active = true
taskQuest.Complete = false

-- 11. TranslationResource
trMessages = TranslationResource("welcome,en,Welcome to Wherigo\nwelcome,es,Bienvenido")

-- 12. Player
playerRef = Wherigo.Player

-- Function to run test actions on OnStart
function cartSampleCartridge:OnStart()
    if cartSampleCartridge.MediaFilePath then
        mediaInfo.Filename = cartSampleCartridge.MediaFilePath
    end

    -- Test type checking function 'made'
    cartSampleCartridge.TestMadeMedia = Wherigo.made(Wherigo.ZMedia, mediaInfo)
    cartSampleCartridge.TestMadeZone = Wherigo.made(Wherigo.Zone, zoneMain)

    -- Test NoCaseEquals
    cartSampleCartridge.TestEquals = Wherigo.NoCaseEquals("JourWigo", "jourwigo")

    -- Test TranslationResource translate method
    cartSampleCartridge.TranslatedWelcome = trMessages:translate("welcome", "en")

    -- Test Container / Thing MoveTo methods
    npcGuide:MoveTo(zoneMain)
    itemKey:MoveTo(Wherigo.Player)

    -- Test Zone methods
    cartSampleCartridge.ZoneContainsResult = zoneMain:Contains(npcGuide)

    -- Test Media methods
    mediaInfo:Play()
    mediaInfo:Stop()
    mediaInfo:Show()
    mediaInfo:Read()

    -- Test Timer methods
    timerCountdown:Start()
    timerCountdown:Stop()
    timerCountdown:Tick()

    -- Test MediaPickerLib functions
    cartSampleCartridge.PhotoResult = Wherigo.SnapPhoto()
    cartSampleCartridge.FileResult = Wherigo.PickFile("image/*")

    -- Test Wherigo helper library functions
    translatedPoint = Wherigo.TranslatePoint(zp1, 100, 45)
    vDist, vBearing = Wherigo.VectorToPoint(zp1, zp2)

    cartSampleCartridge.TranslatedLat = translatedPoint.latitude
    cartSampleCartridge.VectorDist = vDist
    cartSampleCartridge.VectorBearing = vBearing

    Wherigo.ShowStatusText("Running Sample Cartridge Integration Test")
    Wherigo.LogMessage("Integration test OnStart invoked")
    Wherigo.ShowScreen(Wherigo.MAINSCREEN)

    Wherigo.PlayAudio(mediaInfo)
    Wherigo.GetInput(inputQuestion)
    Wherigo.MessageBox{Text = "MessageBox Test"}
    Wherigo.Dialog{{Text = "Dialog Page 1"}, {Text = "Dialog Page 2"}}
    Wherigo.Command("Save")

    cartSampleCartridge.OnStartCalled = true
end

return cartSampleCartridge
