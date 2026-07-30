export interface Subject {
  id: string
  name: string
  color: string
  createdAt: number
}

export interface Chapter {
  id: string
  subjectId: string
  name: string
  completed: boolean
  order: number
  createdAt: number
}

export interface StudyPlan {
  id: string
  subjectId: string
  chapterId: string
  subjectName: string
  chapterName: string
  date: string
  startTime: string
  endTime: string
  createdAt: number
}

export interface StudySession {
  date: string
  hours: number
  chaptersCompleted: number
}
