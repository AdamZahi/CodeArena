import { Injectable } from '@angular/core';
import { TtsService } from './tts.service';
import { StoryMission } from '../models/terminal-quest.model';

@Injectable({ providedIn: 'root' })
export class MissionVoiceService {

  constructor(private tts: TtsService) {}

  playMissionIntro(mission: StoryMission): void {
    this.tts.speak(
      mission.context,
      mission.speakerVoice || 'Charon',
      this.getStyleForMission(mission)
    );
  }

  private getStyleForMission(mission: StoryMission): string {
    if (mission.isBoss) return 'urgent, intense, almost screaming, high adrenaline';
    if (mission.difficulty === 'HARD') return 'serious, focused, tense atmosphere';
    if (mission.difficulty === 'MEDIUM') return 'professional, calm but determined';
    return 'warm, friendly, patient mentor';
  }

  playCorrectAnswer(starsEarned: number): void {
    if (starsEarned === 3) {
      this.tts.speak(
        'Perfect execution. Three stars earned. Outstanding work operator.',
        'Aoede',
        'excited celebratory'
      );
    } else if (starsEarned === 2) {
      this.tts.speak(
        'Well done. Two stars. Solid performance.',
        'Puck',
        'warm congratulatory'
      );
    } else {
      this.tts.speak(
        'Mission complete. Room for improvement but you made it.',
        'Kore',
        'encouraging supportive'
      );
    }
  }

  playWrongAnswer(attempts: number): void {
    if (attempts > 4) {
      this.tts.speak(
        'Multiple failures detected. Use the hint system. Regroup and try again.',
        'Charon',
        'serious warning tone'
      );
    } else {
      this.tts.speak(
        'Incorrect command. Check your syntax and try again.',
        'Kore',
        'calm patient instructor'
      );
    }
  }

  playChapterUnlocked(chapterTitle: string): void {
    this.tts.speak(
      `New chapter unlocked. Welcome to ${chapterTitle}. Prepare for new challenges.`,
      'Fenrir',
      'epic dramatic announcer'
    );
  }

  playBossDefeated(): void {
    this.tts.speak(
      'Boss defeated. Exceptional performance operator. You have proven your skills.',
      'Aoede',
      'triumphant victorious epic'
    );
  }
}
