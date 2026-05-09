"""
AI Weak Code Memory Tracker — FastAPI Microservice
Analyzes student code submissions, detects recurring mistakes,
and builds persistent weakness profiles for adaptive coaching.
"""
from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session

from database import engine, get_db, Base
from schemas import CodeSubmission, WeaknessProfile, AnalysisResult
from memory_service import analyze_and_store, get_weakness_profile, get_recent_mistakes

# Create tables
Base.metadata.create_all(bind=engine)

app = FastAPI(
    title="AI Weak Code Memory Tracker",
    description="Tracks student coding mistakes and builds adaptive weakness profiles",
    version="1.0.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200", "http://localhost:8080", "*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health():
    return {"status": "ok", "service": "ai-memory-tracker"}


@app.post("/api/ai-memory/analyze", response_model=AnalysisResult)
def analyze_code(submission: CodeSubmission, db: Session = Depends(get_db)):
    """Analyze submitted code, detect errors, store in student memory."""
    try:
        result = analyze_and_store(
            db, submission.student_id, submission.language, submission.code
        )
        return result
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/ai-memory/profile/{student_id}", response_model=WeaknessProfile)
def get_profile(student_id: str, db: Session = Depends(get_db)):
    """Get the full weakness profile for a student."""
    try:
        return get_weakness_profile(db, student_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/ai-memory/mistakes/{student_id}")
def get_mistakes(student_id: str, limit: int = 20, db: Session = Depends(get_db)):
    """Get recent mistakes for a student."""
    try:
        return get_recent_mistakes(db, student_id, limit)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.delete("/api/ai-memory/profile/{student_id}")
def reset_profile(student_id: str, db: Session = Depends(get_db)):
    """Reset a student's weakness profile (for testing)."""
    from models import StudentProfile, MistakeRecord
    db.query(MistakeRecord).filter(MistakeRecord.student_id == student_id).delete()
    db.query(StudentProfile).filter(StudentProfile.student_id == student_id).delete()
    db.commit()
    return {"message": f"Profile reset for {student_id}"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
