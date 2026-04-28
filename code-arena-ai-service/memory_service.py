"""
Memory service: manages student weakness profiles, stores mistakes,
calculates improvement trends and generates adaptive recommendations.
"""
from sqlalchemy.orm import Session
from sqlalchemy import func
from datetime import datetime, timedelta
from typing import Dict, List, Optional
from collections import Counter

from models import StudentProfile, MistakeRecord
from error_classifier import classify_errors, get_recommendations


def get_or_create_profile(db: Session, student_id: str) -> StudentProfile:
    """Get existing profile or create a new one."""
    profile = db.query(StudentProfile).filter(
        StudentProfile.student_id == student_id
    ).first()
    if not profile:
        profile = StudentProfile(student_id=student_id, overall_score=100.0)
        db.add(profile)
        db.commit()
        db.refresh(profile)
    return profile


def analyze_and_store(db: Session, student_id: str, language: str, code: str) -> Dict:
    """Analyze submitted code, detect errors, store in memory, return analysis."""
    profile = get_or_create_profile(db, student_id)
    errors = classify_errors(code, language)
    recommendations = get_recommendations(errors, language)

    # Store each detected error
    for error in errors:
        record = MistakeRecord(
            student_id=student_id,
            language=language.upper(),
            error_type=error["error_type"],
            error_category=error["category"],
            code_snippet=error.get("code_line", ""),
            ai_feedback=error["message"],
            severity=error["severity"],
        )
        db.add(record)

    # Update overall score (deduct points per error, min 0)
    severity_penalty = {"low": 0.5, "medium": 1.5, "high": 3.0}
    total_penalty = sum(severity_penalty.get(e["severity"], 1) for e in errors)
    profile.overall_score = max(0, profile.overall_score - total_penalty)
    # Slight recovery if no errors
    if not errors:
        profile.overall_score = min(100, profile.overall_score + 2.0)
    profile.updated_at = datetime.utcnow()

    db.commit()

    return {
        "errors_detected": len(errors) > 0,
        "errors": errors,
        "message": f"Found {len(errors)} issue(s) in your {language} code." if errors
                   else f"No common issues detected in your {language} code!",
        "recommendations": recommendations,
    }


def get_weakness_profile(db: Session, student_id: str) -> Dict:
    """Build a complete weakness profile for a student."""
    profile = get_or_create_profile(db, student_id)
    mistakes = db.query(MistakeRecord).filter(
        MistakeRecord.student_id == student_id
    ).order_by(MistakeRecord.timestamp.desc()).all()

    # Language weakness scores
    lang_counts = Counter(m.language for m in mistakes)
    total = len(mistakes) if mistakes else 1
    language_weaknesses = {lang: round(count / total * 100, 1) for lang, count in lang_counts.items()}

    # Error type frequency
    error_freq = Counter(m.error_type for m in mistakes)

    # Top weaknesses (most frequent errors)
    top_weaknesses = [
        {"error_type": et, "count": c, "category": next(
            (m.error_category for m in mistakes if m.error_type == et), "Unknown"
        )}
        for et, c in error_freq.most_common(5)
    ]

    # Improvement trend (last 7 periods)
    trend = _calculate_trend(mistakes)

    # Adaptive recommendations
    recs = _generate_adaptive_recommendations(error_freq, language_weaknesses, trend)

    return {
        "student_id": student_id,
        "overall_score": round(profile.overall_score, 1),
        "language_weaknesses": language_weaknesses,
        "error_type_frequency": dict(error_freq),
        "improvement_trend": trend,
        "top_weaknesses": top_weaknesses,
        "recommendations": recs,
        "total_mistakes": total if mistakes else 0,
    }


def _calculate_trend(mistakes: List[MistakeRecord]) -> List[Dict]:
    """Calculate error trend over last 7 time periods."""
    if not mistakes:
        return []

    now = datetime.utcnow()
    periods = []
    for i in range(6, -1, -1):
        start = now - timedelta(days=i + 1)
        end = now - timedelta(days=i)
        count = sum(1 for m in mistakes if start <= m.timestamp < end)
        periods.append({
            "date": (now - timedelta(days=i)).strftime("%Y-%m-%d"),
            "errors": count,
        })
    return periods


def _generate_adaptive_recommendations(
    error_freq: Counter, lang_weaknesses: Dict, trend: List[Dict]
) -> List[str]:
    """Generate smart recommendations based on patterns."""
    recs = []
    top_errors = error_freq.most_common(3)

    for error_type, count in top_errors:
        if count >= 3:
            if "semicolon" in error_type.lower():
                recs.append(f"You frequently forget semicolons ({count} times). Focus on syntax drills.")
            elif "join" in error_type.lower():
                recs.append(f"You made the same SQL JOIN mistake {count} times. Here is a targeted exercise.")
            elif "inheritance" in error_type.lower() or "polymorphism" in error_type.lower():
                recs.append(f"Your OOP understanding needs work ({count} errors). Practice inheritance exercises.")
            elif "off-by-one" in error_type.lower():
                recs.append(f"Repeated loop boundary errors ({count}x). Practice off-by-one problems.")
            elif "naming" in error_type.lower():
                recs.append(f"Poor naming habits detected ({count}x). Study naming conventions.")
            else:
                recs.append(f"'{error_type}' keeps recurring ({count}x). Targeted practice recommended.")
        elif count >= 1:
            recs.append(f"Watch for '{error_type}' — appeared {count} time(s) recently.")

    # Trend-based recommendations
    if len(trend) >= 3:
        recent = sum(t["errors"] for t in trend[-3:])
        older = sum(t["errors"] for t in trend[:3])
        if recent < older:
            recs.append("Great progress! Your error rate is declining. Keep it up!")
        elif recent > older:
            recs.append("Your error rate is increasing. Consider reviewing fundamentals.")

    if not recs:
        recs.append("Keep submitting code to build your weakness profile!")

    return recs


def get_recent_mistakes(db: Session, student_id: str, limit: int = 20) -> List[Dict]:
    """Get recent mistakes for a student."""
    records = db.query(MistakeRecord).filter(
        MistakeRecord.student_id == student_id
    ).order_by(MistakeRecord.timestamp.desc()).limit(limit).all()

    return [
        {
            "id": r.id,
            "language": r.language,
            "error_type": r.error_type,
            "error_category": r.error_category,
            "code_snippet": r.code_snippet,
            "ai_feedback": r.ai_feedback,
            "severity": r.severity,
            "timestamp": r.timestamp.isoformat(),
        }
        for r in records
    ]
