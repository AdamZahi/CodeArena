from pydantic import BaseModel
from typing import List, Optional, Dict
from datetime import datetime


class CodeSubmission(BaseModel):
    student_id: str
    language: str
    code: str


class MistakeOut(BaseModel):
    id: int
    language: str
    error_type: str
    error_category: str
    code_snippet: str
    ai_feedback: Optional[str] = None
    severity: str
    timestamp: datetime

    class Config:
        from_attributes = True


class StudentProfileOut(BaseModel):
    student_id: str
    overall_score: float
    created_at: datetime
    updated_at: datetime
    mistakes: List[MistakeOut] = []

    class Config:
        from_attributes = True


class AnalysisResult(BaseModel):
    errors_detected: bool
    errors: List[Dict]
    message: str
    recommendations: List[str]


class WeaknessProfile(BaseModel):
    student_id: str
    overall_score: float
    language_weaknesses: Dict[str, float]
    error_type_frequency: Dict[str, int]
    improvement_trend: List[Dict]
    top_weaknesses: List[Dict]
    recommendations: List[str]
    total_mistakes: int


class ErrorCategory(BaseModel):
    category: str
    count: int
    percentage: float
    trend: str  # improving, declining, stable
