from sqlalchemy import Column, Integer, String, Float, DateTime, ForeignKey, Text, JSON
from sqlalchemy.orm import relationship
import datetime
from database import Base


class StudentProfile(Base):
    __tablename__ = "student_profiles"

    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(String(255), unique=True, index=True)
    overall_score = Column(Float, default=100.0)
    created_at = Column(DateTime, default=datetime.datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.datetime.utcnow, onupdate=datetime.datetime.utcnow)

    mistakes = relationship("MistakeRecord", back_populates="student", cascade="all, delete-orphan")


class MistakeRecord(Base):
    __tablename__ = "mistake_records"

    id = Column(Integer, primary_key=True, index=True)
    student_id = Column(String(255), ForeignKey("student_profiles.student_id"))
    language = Column(String(50), index=True)
    error_type = Column(String(100), index=True)
    error_category = Column(String(100), index=True)
    code_snippet = Column(Text)
    ai_feedback = Column(Text, nullable=True)
    severity = Column(String(20), default="medium")  # low, medium, high
    timestamp = Column(DateTime, default=datetime.datetime.utcnow)

    student = relationship("StudentProfile", back_populates="mistakes")
