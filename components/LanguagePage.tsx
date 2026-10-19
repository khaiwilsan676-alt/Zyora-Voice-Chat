'use client'

import React, { useState, useEffect } from 'react'
import { ArrowLeft } from 'lucide-react'
import { translations, LanguageCode } from '../lib/translations'

interface LanguagePageProps {
  onBack: () => void
}

const LANGUAGES = [
  { code: 'en', label: 'English', native: 'English' },
  { code: 'hi', label: 'Hindi', native: 'हिन्दी' },
  { code: 'bn', label: 'Bengali', native: 'বাংলা' },
  { code: 'ar', label: 'Arabic', native: 'العربية' },
  { code: 'ur', label: 'Urdu', native: 'اردو' },
  { code: 'tr', label: 'Turkish', native: 'Türkçe' },
  { code: 'es', label: 'Spanish', native: 'Español' },
  { code: 'fr', label: 'French', native: 'Français' },
  { code: 'de', label: 'German', native: 'Deutsch' },
  { code: 'pt', label: 'Portuguese', native: 'Português' },
  { code: 'ru', label: 'Russian', native: 'Русский' },
  { code: 'id', label: 'Indonesian', native: 'Bahasa Indonesia' },
  { code: 'ja', label: 'Japanese', native: '日本語' },
  { code: 'ko', label: 'Korean', native: '한국어' },
  { code: 'zh', label: 'Chinese', native: '中文' },
]

export default function LanguagePage({ onBack }: LanguagePageProps) {
  const [selectedLang, setSelectedLang] = useState<LanguageCode>('en')
  const [isClosing, setIsClosing] = useState(false)
  const [pendingLang, setPendingLang] = useState<LanguageCode>('en')

  useEffect(() => {
    const savedLang = localStorage.getItem('appLanguage') as LanguageCode
    if (savedLang && Object.keys(translations).includes(savedLang)) {
      setSelectedLang(savedLang)
      setPendingLang(savedLang)
    }
  }, [])

  const handleLanguageSelect = (code: string) => {
    const langCode = code as LanguageCode
    setPendingLang(langCode)
  }

  const handleSave = () => {
    setSelectedLang(pendingLang)
    localStorage.setItem('appLanguage', pendingLang)

    // Dispatch custom event to update whole app pages language
    window.dispatchEvent(new CustomEvent('languageChange', { detail: { lang: pendingLang } }))

    handleClose()
  }

  const handleClose = () => {
    setIsClosing(true)
    onBack()
  }

  const t = translations[selectedLang]

  return (
    <div className="fixed inset-0 z-[100] flex flex-col bg-white">
      {/* Content */}
      <div
        className="relative bg-white w-full h-full overflow-hidden flex flex-col"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex flex-col h-full">
          {/* Header with Android Status Bar spacing & Save button */}
          <div className="flex items-center justify-between px-3 py-4 pt-6 border-b border-gray-100 bg-white">
            <button
              onClick={handleClose}
              className="p-2 hover:bg-gray-100 rounded-full transition-colors"
            >
              <ArrowLeft size={24} className="text-gray-900" />
            </button>
            <h2 className="text-lg font-bold text-gray-900">{t.languageSetting || 'Language'}</h2>
            <button
              onClick={handleSave}
              className="text-blue-600 font-semibold text-sm px-3 py-1.5 hover:bg-blue-50 rounded-lg transition-colors"
            >
              Save
            </button>
          </div>

          {/* Language List - No Cards */}
          <div className="overflow-y-auto px-4 py-2 flex-1 hide-scrollbar">
            <div className="divide-y divide-gray-100 mt-2 mb-6">
              {LANGUAGES.map((lang) => (
                <div
                  key={lang.code}
                  className="flex items-center justify-between py-4 px-2 cursor-pointer hover:bg-gray-50 transition-colors"
                  onClick={() => handleLanguageSelect(lang.code)}
                >
                  <div className="flex flex-col">
                    <span className="font-semibold text-gray-900">{lang.native}</span>
                    <span className="text-xs text-gray-500 mt-0.5">{lang.label}</span>
                  </div>

                  <div className="w-6 h-6 rounded-full border-2 border-gray-300 flex items-center justify-center transition-colors">
                    {pendingLang === lang.code && (
                      <div className="w-3 h-3 bg-blue-500 rounded-full" />
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>

      <style>{`
        .hide-scrollbar {
          -ms-overflow-style: none;
          scrollbar-width: none;
        }
        .hide-scrollbar::-webkit-scrollbar {
          display: none;
        }
      `}</style>
    </div>
  )
}
