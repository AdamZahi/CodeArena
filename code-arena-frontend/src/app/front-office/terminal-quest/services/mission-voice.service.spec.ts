import { TestBed } from '@angular/core/testing';
import { MissionVoiceService } from './mission-voice.service';
import { TtsService } from './tts.service';
import { StoryMission } from '../models/terminal-quest.model';

describe('MissionVoiceService', () => {
  let service: MissionVoiceService;
  let ttsSpy: jasmine.SpyObj<TtsService>;

  const baseMission = {
    id: 'm1', chapterId: 'c1', title: 'Test Mission', context: 'A mission context',
    task: 'Do something', hint: '', orderIndex: 1, difficulty: 'EASY',
    isBoss: false, xpReward: 100, speakerVoice: 'Charon', speakerName: 'Sarah',
    createdAt: '2024-01-01'
  } as StoryMission;

  beforeEach(() => {
    ttsSpy = jasmine.createSpyObj('TtsService', ['speak', 'stop']);

    TestBed.configureTestingModule({
      providers: [
        MissionVoiceService,
        { provide: TtsService, useValue: ttsSpy }
      ]
    });
    service = TestBed.inject(MissionVoiceService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call tts.speak with boss style for boss mission', () => {
    const mission = { ...baseMission, isBoss: true } as StoryMission;
    service.playMissionIntro(mission);
    expect(ttsSpy.speak).toHaveBeenCalledWith(
      mission.context,
      'Charon',
      'urgent, intense, almost screaming, high adrenaline'
    );
  });

  it('should call tts.speak with HARD style', () => {
    const mission = { ...baseMission, difficulty: 'HARD', isBoss: false } as StoryMission;
    service.playMissionIntro(mission);
    expect(ttsSpy.speak).toHaveBeenCalledWith(
      mission.context, 'Charon', 'serious, focused, tense atmosphere'
    );
  });

  it('should call tts.speak with MEDIUM style', () => {
    const mission = { ...baseMission, difficulty: 'MEDIUM', isBoss: false } as StoryMission;
    service.playMissionIntro(mission);
    expect(ttsSpy.speak).toHaveBeenCalledWith(
      mission.context, 'Charon', 'professional, calm but determined'
    );
  });

  it('should call tts.speak with friendly style for EASY mission', () => {
    service.playMissionIntro(baseMission);
    expect(ttsSpy.speak).toHaveBeenCalledWith(
      baseMission.context, 'Charon', 'warm, friendly, patient mentor'
    );
  });

  it('should speak 3-star message for starsEarned=3', () => {
    service.playCorrectAnswer(3);
    expect(ttsSpy.speak).toHaveBeenCalledWith(
      'Perfect execution. Three stars earned. Outstanding work operator.',
      'Aoede', 'excited celebratory'
    );
  });

  it('should speak 2-star message for starsEarned=2', () => {
    service.playCorrectAnswer(2);
    expect(ttsSpy.speak).toHaveBeenCalledWith(
      'Well done. Two stars. Solid performance.',
      'Puck', 'warm congratulatory'
    );
  });

  it('should speak 1-star message for starsEarned=1', () => {
    service.playCorrectAnswer(1);
    expect(ttsSpy.speak).toHaveBeenCalledWith(
      'Mission complete. Room for improvement but you made it.',
      'Kore', 'encouraging supportive'
    );
  });

  it('should speak warning for attempts > 4', () => {
    service.playWrongAnswer(5);
    expect(ttsSpy.speak).toHaveBeenCalledWith(
      'Multiple failures detected. Use the hint system. Regroup and try again.',
      'Charon', 'serious warning tone'
    );
  });

  it('should speak calm instructor for attempts <= 4', () => {
    service.playWrongAnswer(2);
    expect(ttsSpy.speak).toHaveBeenCalledWith(
      'Incorrect command. Check your syntax and try again.',
      'Kore', 'calm patient instructor'
    );
  });

  it('should speak chapter unlocked announcement', () => {
    service.playChapterUnlocked('Linux Basics');
    expect(ttsSpy.speak).toHaveBeenCalledWith(
      'New chapter unlocked. Welcome to Linux Basics. Prepare for new challenges.',
      'Fenrir', 'epic dramatic announcer'
    );
  });

  it('should speak boss defeated message', () => {
    service.playBossDefeated();
    expect(ttsSpy.speak).toHaveBeenCalledWith(
      'Boss defeated. Exceptional performance operator. You have proven your skills.',
      'Aoede', 'triumphant victorious epic'
    );
  });
});
