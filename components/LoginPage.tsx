'use client'

import { apiUrl } from "../src/lib/api";

import { useState, useEffect, useRef } from 'react'
import { Device } from '@capacitor/device'
import { socket } from "../src/lib/socket"
import { ArrowLeft, ArrowRight, Eye, EyeOff, User } from 'lucide-react'
import { 
  GoogleAuthProvider, 
  signInWithEmailAndPassword, 
  createUserWithEmailAndPassword 
} from "firebase/auth";
import { FirebaseAuthentication } from "@capacitor-firebase/authentication";
import { auth, googleProvider as provider } from "../src/lib/firebase";

interface LoginPageProps {
  onLoginSuccess?: (data?: any) => void
}

// Special Accounts Mapping
const SPECIAL_ACCOUNTS: { [key: string]: string } = {
  'HUSxSvQnabgU029dWYt1TUV04hd2': '100002',
  'ADqW31RGBMaosOzy0HiqexKSD7h1': '100003',
  '100002': '100002',
  '100003': '100003'
}

// Official & Admin IDs List
const OFFICIAL_IDS = ['500001', '500002', '500003', '500004', '500005']
const ADMIN_IDS = ['700001', '700002', '700003']


// Country Options
const COUNTRIES = [
  { code: 'IN', name: 'India', flag: '🇮🇳' },
  { code: 'US', name: 'United States', flag: '🇺🇸' },
  { code: 'GB', name: 'United Kingdom', flag: '🇬🇧' },
  { code: 'CA', name: 'Canada', flag: '🇨🇦' },
  { code: 'AU', name: 'Australia', flag: '🇦🇺' },
  { code: 'AE', name: 'UAE', flag: '🇦🇪' },
  { code: 'SA', name: 'Saudi Arabia', flag: '🇸🇦' },
  { code: 'PK', name: 'Pakistan', flag: '🇵🇰' },
  { code: 'BD', name: 'Bangladesh', flag: '🇧🇩' },
  { code: 'NP', name: 'Nepal', flag: '🇳🇵' },
  { code: 'LK', name: 'Sri Lanka', flag: '🇱🇰' },
  { code: 'SG', name: 'Singapore', flag: '🇸🇬' },
  { code: 'MY', name: 'Malaysia', flag: '🇲🇾' },
  { code: 'ID', name: 'Indonesia', flag: '🇮🇩' },
  { code: 'PH', name: 'Philippines', flag: '🇵🇭' },
  { code: 'JP', name: 'Japan', flag: '🇯🇵' },
  { code: 'KR', name: 'South Korea', flag: '🇰🇷' },
  { code: 'CN', name: 'China', flag: '🇨🇳' },
  { code: 'DE', name: 'Germany', flag: '🇩🇪' },
  { code: 'FR', name: 'France', flag: '🇫🇷' },
  { code: 'IT', name: 'Italy', flag: '🇮🇹' },
  { code: 'ES', name: 'Spain', flag: '🇪🇸' },
  { code: 'BR', name: 'Brazil', flag: '🇧🇷' },
  { code: 'MX', name: 'Mexico', flag: '🇲🇽' },
  { code: 'ZA', name: 'South Africa', flag: '🇿🇦' },
  { code: 'NG', name: 'Nigeria', flag: '🇳🇬' },
  { code: 'EG', name: 'Egypt', flag: '🇪🇬' },
  { code: 'RU', name: 'Russia', flag: '🇷🇺' },
  { code: 'TR', name: 'Turkey', flag: '🇹🇷' },
  { code: 'NL', name: 'Netherlands', flag: '🇳🇱' },
]

// HELPER: Account ID Generator
export const getOrCreateAccountNumber = (uid: string) => {
  if (!uid || uid === 'N/A') return '100379620'

  if (OFFICIAL_IDS.includes(uid) || ADMIN_IDS.includes(uid)) {
    return uid
  }

  if (SPECIAL_ACCOUNTS[uid]) {
    return SPECIAL_ACCOUNTS[uid]
  }

  let hash = 0
  for (let i = 0; i < uid.length; i++) {
    hash = (hash << 5) - hash + uid.charCodeAt(i)
    hash |= 0
  }
  const positiveHash = Math.abs(hash)
  return String(10000000 + (positiveHash % 90000000))
}

const getUserFromMongoDB = async (uid: string) => {
  if (!uid) return null;

  const response = await fetch(
    apiUrl(`/api/users?uid=${encodeURIComponent(uid)}`)
  );

  if (response.status === 404) {
    return null;
  }

  if (!response.ok) {
    throw new Error(`MongoDB user fetch failed: ${response.status}`);
  }

  const result = await response.json();
  return result?.user || null;
};

const saveUserToMongoDB = async (userData: any) => {
  const uid = userData.uid || userData.id || userData.appLongId;

  if (!uid) {
    throw new Error("Missing user uid");
  }

  const response = await fetch(apiUrl("/api/users"), {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      ...userData,
      uid,
    }),
  });

  if (!response.ok) {
    throw new Error(`MongoDB user save failed: ${response.status}`);
  }

  return response.json();
};

// HELPER: Sync and Lock User Profile in MongoDB
const syncUserToMongoDB = async (uid: string, name: string, email: string, photo: string) => {
  try {
    let finalAccountId = ""
    let existingName = ""
    let existingImage = ""
    let existingBio = ""
    let existingCountry = ""

    const res = await getUserFromMongoDB(uid)
    const existingUser = res && (res.user || res.data || res)

    let existingEmail = ""

    if (existingUser && (existingUser.id || existingUser.AppLongId || existingUser['App long ID'] || existingUser.email || existingUser.gmail || existingUser.Name || existingUser.name)) {
      finalAccountId = String(existingUser.accountId || existingUser.accountNumber || existingUser['Account Number'] || '')
      existingName = existingUser.name || existingUser.Name || ''
      existingImage = existingUser.image || existingUser.avatar || existingUser.Avtar || existingUser.photo || ''
      existingBio = existingUser.bio || existingUser.Bio || ''
      existingCountry = existingUser.country || existingUser.Country || ''
      existingEmail = existingUser.email || existingUser.gmail || ''
    }

    if (OFFICIAL_IDS.includes(uid) || ADMIN_IDS.includes(uid) || SPECIAL_ACCOUNTS[uid]) {
      finalAccountId = SPECIAL_ACCOUNTS[uid] || uid
    } else if (!finalAccountId) {
      finalAccountId = getOrCreateAccountNumber(uid)
    }

    const finalEmail = email || existingEmail || ''
    const finalName = existingName || name || (finalEmail ? finalEmail.split('@')[0] : 'User')
    const finalImage = existingImage || photo || '/default-avatar.png'
    const finalCountry = existingCountry || '🇮🇳'


    let deviceId = '';
    try {
      const deviceIdInfo = await Device.getId();
      deviceId = deviceIdInfo.identifier;
    } catch(e) {}

    const userData: any = {
      id: uid,
      appLongId: uid,
      name: finalName,
      email: finalEmail,
      gmail: finalEmail,
      country: finalCountry,
      image: finalImage,
      avatar: finalImage,
      accountId: finalAccountId,
      accountNumber: finalAccountId,
      bio: existingBio || '',
      deviceId: deviceId
    }

    await saveUserToMongoDB(userData)

    localStorage.setItem("accountNumber", finalAccountId)
    localStorage.setItem(`user_account_number_${uid}`, finalAccountId)

    return { accountId: finalAccountId, name: finalName, image: finalImage }
  } catch (err) {
    console.error("Error syncing user to MongoDB:", err)
    return { accountId: getOrCreateAccountNumber(uid), name: name || email.split('@')[0] || 'User', image: photo || '/default-avatar.png' }
  }
}

// HELPER: Check if user is new (hasn't selected gender yet)
const checkIfNewUser = async (uid: string): Promise<boolean> => {
  try {
    const res = await getUserFromMongoDB(uid)
    const userData = res && (res.user || res.data || res)
    if (userData && (userData.id || userData.AppLongId || userData['App long ID'] || userData.Name || userData.name)) {
      const gender = userData.gender || userData.Gender
      return !gender
    }
    return true
  } catch (error) {
    console.error("Error checking if new user:", error)
    const localGender = localStorage.getItem("userGender")
    return !localGender
  }
}

// Gender Selection Page Component
function GenderSelectionPage({ 
  userData, 
  onContinue 
}: { 
  userData: any, 
  onContinue: (gender: string) => void 
}) {
  const [selectedGender, setSelectedGender] = useState<string>('')

  return (
    <div className="min-h-screen bg-white flex flex-col">
      <div className="px-4 pt-12 pb-6">
        <h1 className="text-2xl font-bold text-gray-900 text-center">Welcome! 🎉</h1>
        <p className="text-sm text-gray-500 text-center mt-2">Select your gender to get started</p>
      </div>

      <div className="flex-1 flex flex-col items-center justify-center px-4 gap-4">
        <button
          onClick={() => setSelectedGender('female')}
          className={`relative w-full max-w-xs transition-all duration-300 transform ${
            selectedGender === 'female' ? 'scale-105' : 'hover:scale-102'
          }`}
        >
          <div 
            className={`relative rounded-2xl overflow-hidden cursor-pointer ${
              selectedGender === 'female' 
                ? 'ring-4 ring-pink-400 shadow-2xl shadow-pink-400/30' 
                : 'ring-2 ring-gray-200 shadow-lg hover:shadow-xl'
            }`}
            style={{
              background: 'linear-gradient(135deg, rgba(236, 72, 153, 0.15) 0%, rgba(244, 114, 182, 0.2) 50%, rgba(251, 207, 232, 0.3) 100%)',
            }}
          >
            <div className="flex items-center gap-4 p-4">
              <div className="w-20 h-20 rounded-full overflow-hidden border-4 border-pink-300 shadow-lg flex-shrink-0">
                <img 
                  src="/IMG_20260804_211013.jpg" 
                  alt="Female" 
                  className="w-full h-full object-cover"
                />
              </div>
              
              <div className="flex-1 text-left">
                <div className="inline-block bg-pink-500/80 backdrop-blur-sm px-5 py-2 rounded-full">
                  <span className="text-white font-semibold text-base">Female</span>
                </div>
              </div>
              
              {selectedGender === 'female' && (
                <div className="absolute top-3 right-3 w-7 h-7 bg-pink-500 rounded-full flex items-center justify-center shadow-lg">
                  <svg className="w-4 h-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                  </svg>
                </div>
              )}
            </div>
          </div>
        </button>

        <button
          onClick={() => setSelectedGender('male')}
          className={`relative w-full max-w-xs transition-all duration-300 transform ${
            selectedGender === 'male' ? 'scale-105' : 'hover:scale-102'
          }`}
        >
          <div 
            className={`relative rounded-2xl overflow-hidden cursor-pointer ${
              selectedGender === 'male' 
                ? 'ring-4 ring-blue-400 shadow-2xl shadow-blue-400/30' 
                : 'ring-2 ring-gray-200 shadow-lg hover:shadow-xl'
            }`}
            style={{
              background: 'linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(96, 165, 250, 0.2) 50%, rgba(191, 219, 254, 0.3) 100%)',
            }}
          >
            <div className="flex items-center gap-4 p-4">
              <div className="w-20 h-20 rounded-full overflow-hidden border-4 border-blue-300 shadow-lg flex-shrink-0">
                <img 
                  src="/IMG_20260804_211031.jpg" 
                  alt="Male" 
                  className="w-full h-full object-cover"
                />
              </div>
              
              <div className="flex-1 text-left">
                <div className="inline-block bg-blue-500/80 backdrop-blur-sm px-5 py-2 rounded-full">
                  <span className="text-white font-semibold text-base">Male</span>
                </div>
              </div>
              
              {selectedGender === 'male' && (
                <div className="absolute top-3 right-3 w-7 h-7 bg-blue-500 rounded-full flex items-center justify-center shadow-lg">
                  <svg className="w-4 h-4 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" />
                  </svg>
                </div>
              )}
            </div>
          </div>
        </button>
      </div>

      <div className="px-4 pb-8 pt-4">
        <button
          onClick={() => selectedGender && onContinue(selectedGender)}
          disabled={!selectedGender}
          className="w-full bg-gradient-to-r from-blue-600 to-blue-700 text-white font-bold py-4 rounded-2xl transition-all hover:from-blue-700 hover:to-blue-800 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer shadow-lg shadow-blue-600/20 flex items-center justify-center gap-3"
        >
          <span className="text-lg">Next</span>
          <ArrowRight size={24} />
        </button>
      </div>
    </div>
  )
}

// Country Selection Page Component
function CountrySelectionPage({ 
  userData,
  selectedGender,
  onComplete 
}: { 
  userData: any,
  selectedGender: string,
  onComplete: (gender: string, country: string) => void 
}) {
  const [selectedCountry, setSelectedCountry] = useState<string>('IN')
  const [showCountryPicker, setShowCountryPicker] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleCountrySelect = (countryCode: string) => {
    setSelectedCountry(countryCode)
    setShowCountryPicker(false)
  }

  const handleComplete = async () => {
    if (isSubmitting) return
    
    setIsSubmitting(true)
    
    try {
      const defaultData = selectedGender === 'female' 
        ? {
            name: 'Barrey',
            image: '/IMG_20260804_211013.jpg',
            gender: 'female'
          }
        : {
            name: 'Simpson',
            image: '/IMG_20260804_211031.jpg',
            gender: 'male'
          }

      const countryFlag = COUNTRIES.find(c => c.code === selectedCountry)?.flag || '🇮🇳'

      if (userData?.id || userData?.uid) {
        const userId = userData.id || userData.uid
        
        const userDocData = {
          id: userId,
          appLongId: userId,
          name: defaultData.name,
          image: defaultData.image,
          avatar: defaultData.image,
          gender: selectedGender,
          country: countryFlag,
          countryCode: selectedCountry,
          email: userData.email || '',
          accountId: localStorage.getItem("accountNumber") || getOrCreateAccountNumber(userId),
          accountNumber: localStorage.getItem("accountNumber") || getOrCreateAccountNumber(userId),
          setupComplete: true
        }

        await saveUserToMongoDB(userDocData)

        localStorage.setItem("userName", defaultData.name)
        localStorage.setItem("userPhoto", defaultData.image)
        localStorage.setItem("userGender", selectedGender)
        localStorage.setItem("userCountry", countryFlag)
        localStorage.setItem("userCountryCode", selectedCountry)
        localStorage.setItem("isNewUser", "false")
        localStorage.setItem("setupComplete", "true")
      }

      onComplete(selectedGender, selectedCountry)
    } catch (error) {
      console.error("Error updating gender:", error)
      onComplete(selectedGender, selectedCountry)
    } finally {
      setIsSubmitting(false)
    }
  }

  const selectedCountryData = COUNTRIES.find(c => c.code === selectedCountry)

  return (
    <div className="min-h-screen bg-white flex flex-col">
      <div className="flex items-center p-4">
        <button
          onClick={() => window.history.back()}
          className="p-2 hover:bg-gray-100 rounded-full transition-colors cursor-pointer"
        >
          <ArrowLeft size={24} className="text-gray-700" />
        </button>
      </div>

      <div className="flex-1 flex flex-col px-4">
        <div className="text-center mb-8 pt-4">
          <h1 className="text-2xl font-bold text-gray-900">Select Your Country</h1>
          <p className="text-sm text-gray-500 mt-2">Choose your location to continue</p>
        </div>

        <div className="max-w-sm mx-auto w-full mb-8">
          <button
            onClick={() => setShowCountryPicker(!showCountryPicker)}
            className="w-full flex items-center justify-between px-5 py-4 border-2 border-gray-200 rounded-2xl bg-white hover:border-gray-300 transition-colors cursor-pointer shadow-sm"
          >
            <div className="flex items-center gap-3">
              <span className="text-3xl">{selectedCountryData?.flag}</span>
              <span className="text-gray-800 font-medium text-lg">{selectedCountryData?.name}</span>
            </div>
            <svg className="w-6 h-6 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
            </svg>
          </button>

          {showCountryPicker && (
            <div className="mt-2 border-2 border-gray-200 rounded-2xl bg-white max-h-64 overflow-y-auto shadow-xl z-50">
              {COUNTRIES.map((country) => (
                <button
                  key={country.code}
                  onClick={() => handleCountrySelect(country.code)}
                  className={`w-full flex items-center gap-3 px-5 py-3.5 hover:bg-gray-50 transition-colors cursor-pointer ${
                    selectedCountry === country.code ? 'bg-blue-50' : ''
                  }`}
                >
                  <span className="text-2xl">{country.flag}</span>
                  <span className={`text-base ${selectedCountry === country.code ? 'text-blue-600 font-semibold' : 'text-gray-700'}`}>
                    {country.name}
                  </span>
                  {selectedCountry === country.code && (
                    <svg className="w-5 h-5 text-blue-500 ml-auto" fill="currentColor" viewBox="0 0 20 20">
                      <path fillRule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clipRule="evenodd" />
                    </svg>
                  )}
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="px-4 pb-8">
        <div className="max-w-sm mx-auto">
          <button
            onClick={handleComplete}
            disabled={isSubmitting}
            className="w-full bg-gradient-to-r from-blue-600 to-blue-700 text-white font-bold py-4 rounded-2xl transition-all hover:from-blue-700 hover:to-blue-800 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer shadow-lg shadow-blue-600/20 flex items-center justify-center gap-3"
          >
            {isSubmitting ? (
              <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
            ) : (
              <>
                <span className="text-lg">Let's Go!</span>
                <ArrowRight size={24} />
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  )
}

export default function LoginPage({ onLoginSuccess }: LoginPageProps) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [authError, setAuthError] = useState<string | null>(null)
  const [showPassword, setShowPassword] = useState(false)
  const [showPrivacyPolicy, setShowPrivacyPolicy] = useState(false)
  const [showLoginPage, setShowLoginPage] = useState(false)
  const [showSignUpPage, setShowSignUpPage] = useState(false)
  const [showGenderPage, setShowGenderPage] = useState(false)
  const [showCountryPage, setShowCountryPage] = useState(false)
  const [videoLoaded, setVideoLoaded] = useState(false)
  const [pendingUserData, setPendingUserData] = useState<any>(null)
  const [pendingGender, setPendingGender] = useState<string>('')
  const [checkingNewUser, setCheckingNewUser] = useState(false)
  const [banMessage, setBanMessage] = useState<string | null>(null)


  // Preload video
  useEffect(() => {
    const video = document.createElement('video');
    video.src = '/VID_20260804_011114_027_bsl.mp4';
    video.preload = 'auto';
    video.muted = true;
  }, []);

  // Check recent ban on mount
  useEffect(() => {
    const recentBanStr = localStorage.getItem('recentBanMessage');
    if (recentBanStr) {
      try {
        const banData = JSON.parse(recentBanStr);
        const type = banData.type || 'Violation';
        let unbanTimeStr = 'Never';
        if (banData.unbanTime && banData.unbanTime !== -1) {
          unbanTimeStr = new Date(banData.unbanTime).toLocaleString();
        }
        setBanMessage(`Your Account Has been ban Due to ${type}\nUnban Time: ${unbanTimeStr}`);
      } catch (e) {}
      localStorage.removeItem('recentBanMessage');
    }
  }, []);

  // Auto close ban message after 3 seconds
  useEffect(() => {
    if (banMessage) {
      const timer = setTimeout(() => setBanMessage(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [banMessage]);

  // Pre-login ban check (device based) - called on button clicks
  const checkBanBeforeLogin = async (): Promise<boolean> => {
    try {
      const deviceIdInfo = await Device.getId();
      const deviceId = deviceIdInfo.identifier;
      const banRes = await fetch(apiUrl('/api/check-ban'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ deviceId })
      });
      if (banRes.ok) {
        const banData = await banRes.json();
        if (banData.banned) {
          const type = banData.banData.type || 'Violation';
          let unbanTimeStr = 'Never';
          if (banData.banData.unbanTime !== -1) {
            unbanTimeStr = new Date(banData.banData.unbanTime).toLocaleString();
          }
          setBanMessage(`Your Account Has been ban Due to ${type}\nUnban Time: ${unbanTimeStr}`);
          return true;
        }
      }
    } catch (e) {
      console.error("Pre-login ban check failed:", e);
    }
    return false;
  };

  const handleGenderContinue = (gender: string) => {
    setPendingGender(gender)
    setShowGenderPage(false)
    setShowCountryPage(true)
  }

  const handleGenderComplete = (gender: string, country: string) => {
    setShowCountryPage(false)
    if (pendingUserData && onLoginSuccess) {
      onLoginSuccess(pendingUserData)
    }
  }

  const processLoginSuccess = async (userData: any) => {
    const userId = userData?.id || userData?.uid
    // BAN CHECK
    try {
      const deviceIdInfo = await Device.getId();
      const deviceId = deviceIdInfo.identifier;
      const accountId = userData.accountId || userData.accountNumber || (userData.uid ? getOrCreateAccountNumber(userData.uid) : 'N/A');

      const banRes = await fetch(apiUrl('/api/check-ban'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ accountId, deviceId })
      });
      if (banRes.ok) {
        const banData = await banRes.json();
        if (banData.banned) {
          const type = banData.banData.type || 'Violation';
          let unbanTimeStr = 'Never';
          if (banData.banData.unbanTime !== -1) {
            unbanTimeStr = new Date(banData.banData.unbanTime).toLocaleString();
          }
          setBanMessage(`Your Account Has been ban Due to ${type}\nUnban Time: ${unbanTimeStr}`);
          setLoading(false);
          return; // Stop login
        }
      }
    } catch (e) {
      console.error("Ban check failed:", e);
    }
    
    if (!userId) {
      if (onLoginSuccess) onLoginSuccess(userData)
      return
    }

    setCheckingNewUser(true)
    
    try {
      const isNew = await checkIfNewUser(userId)
      
      if (isNew) {
        setPendingUserData(userData)
        setShowGenderPage(true)
      } else {
        const res = await getUserFromMongoDB(userId)
        const existingData = res && (res.user || res.data || res)
        if (existingData) {
          const name = existingData.name || existingData.Name
          const avatar = existingData.avatar || existingData.Avtar || existingData.image || existingData.photo
          const gender = existingData.gender || existingData.Gender
          const country = existingData.country || existingData.Country
          const countryCode = existingData.countryCode
          if (name) localStorage.setItem("userName", name)
          if (avatar) localStorage.setItem("userPhoto", avatar)
          if (gender) localStorage.setItem("userGender", gender)
          if (country) localStorage.setItem("userCountry", country)
          if (countryCode) localStorage.setItem("userCountryCode", countryCode)
        }
        
        if (onLoginSuccess) {
          onLoginSuccess(userData)
        }
      }
    } catch (error) {
      console.error("Error checking new user status:", error)
      const localGender = localStorage.getItem("userGender")
      if (!localGender) {
        setPendingUserData(userData)
        setShowGenderPage(true)
      } else {
        if (onLoginSuccess) onLoginSuccess(userData)
      }
    } finally {
      setCheckingNewUser(false)
    }
  }

  // Direct Google Login - No sheet
  const handleGoogleLogin = async () => {
    setAuthError(null);

    // First, check ban before proceeding
    const isBanned = await checkBanBeforeLogin();
    if (isBanned) {
      return; // show ban message, stop login
    }

    setLoading(true);

    try {
      const result = await FirebaseAuthentication.signInWithGoogle();
      const user = result.user;

      if (!user?.uid) {
        throw new Error("Google sign-in failed: user information not received.");
      }

      const userName = user.displayName || "Google User";
      const userEmail = user.email || "";
      const userPhoto = user.photoUrl || "/default-avatar.png";
      const userUID = user.uid;

      const syncResult = await syncUserToMongoDB(
        userUID,
        userName,
        userEmail,
        userPhoto
      );

      localStorage.setItem("userName", syncResult.name);
      localStorage.setItem("userEmail", userEmail);
      localStorage.setItem("userPhoto", syncResult.image);
      localStorage.setItem("userUID", userUID);
      localStorage.setItem("accountNumber", syncResult.accountId);

      await processLoginSuccess({
        uid: userUID,
        id: userUID,
        displayName: userName,
        email: userEmail,
        photoURL: userPhoto,
      });

    } catch (error: any) {
      console.error("Google login error:", error);
      let errorMessage = "Google sign-in failed. Please try again.";
      if (error?.message?.toLowerCase().includes("cancel")) {
        errorMessage = "Google sign-in was cancelled.";
      } else if (error?.message) {
        errorMessage = error.message;
      }
      setAuthError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  // Login with Account button click handler (with ban pre-check)
  const handleLoginWithAccountClick = async () => {
    const isBanned = await checkBanBeforeLogin();
    if (isBanned) {
      return; // show ban message, don't open login page
    }
    setShowLoginPage(true);
  };

  const checkOfficialCredentials = async (emailStr: string, passwordStr: string) => {
    try {
      const savedCredentials = localStorage.getItem('officialCredentials');
      if (savedCredentials) {
        const credentials = JSON.parse(savedCredentials);
        const matched = credentials.find(
          (cred: any) => cred.email === emailStr && cred.password === passwordStr
        );
        return matched || null;
      }
    } catch (error) {
      console.error("Error checking official credentials:", error);
    }
    return null;
  };

  const handleEmailAuth = async (e: React.FormEvent) => {
    e.preventDefault();
    setAuthError(null);
    
    if (!email || !password || password.length < 6) {
      setAuthError("Password must be at least 6 characters long.");
      return;
    }

    setLoading(true);

    try {
      // Check official credentials first
      const officialCred = await checkOfficialCredentials(email, password);
      
      if (officialCred) {
        const userName = `${officialCred.type.toUpperCase()} - ${officialCred.id}`
        const officialID = officialCred.id

        const syncResult = await syncUserToMongoDB(officialID, userName, officialCred.email, "")

        const userData = {
          id: officialID,
          email: officialCred.email,
          type: officialCred.type,
          isOfficial: true
        };

        localStorage.setItem("userName", syncResult.name);
        localStorage.setItem("userEmail", officialCred.email);
        localStorage.setItem("userUID", officialID);
        localStorage.setItem("accountNumber", syncResult.accountId);
        localStorage.setItem("userType", officialCred.type);
        localStorage.setItem("userPhoto", syncResult.image);

        const loggedInSessions = JSON.parse(localStorage.getItem('loggedInSessions') || '{}')
        loggedInSessions[officialID] = true
        localStorage.setItem('loggedInSessions', JSON.stringify(loggedInSessions));

        if (onLoginSuccess) {
          onLoginSuccess(userData);
        }
        setLoading(false);
        return;
      }

      // Try Firebase Auth login
      try {
        const userCredential = await signInWithEmailAndPassword(auth, email, password);
        const user = userCredential.user;
        const userName = user.displayName || email.split('@')[0]
        const userEmail = user.email || ""
        const userPhoto = user.photoURL || "/default-avatar.png"
        const userUID = user.uid

        const syncResult = await syncUserToMongoDB(userUID, userName, userEmail, userPhoto)

        localStorage.setItem("userName", syncResult.name);
        localStorage.setItem("userEmail", userEmail);
        localStorage.setItem("userPhoto", syncResult.image);
        localStorage.setItem("userUID", userUID);
        localStorage.setItem("accountNumber", syncResult.accountId);

        await processLoginSuccess(user);
      } catch (signInError: any) {
        console.error('Sign in error details:', {
          code: signInError.code,
          message: signInError.message,
          email: email
        });

        if (signInError.code === 'auth/user-not-found' || 
            signInError.code === 'auth/invalid-credential' ||
            signInError.code === 'auth/wrong-password') {
          setAuthError("Invalid email or password. Please try again or sign up.");
        } else {
          throw signInError;
        }
      }
    } catch (error: any) {
      console.error('Auth error:', error);
      let errorMessage = "Authentication failed. Please check your credentials.";
      
      switch (error.code) {
        case 'auth/invalid-email':
          errorMessage = "Invalid email format.";
          break;
        case 'auth/user-disabled':
          errorMessage = "This account has been disabled.";
          break;
        case 'auth/user-not-found':
          errorMessage = "No account found with this email. Please sign up.";
          break;
        case 'auth/wrong-password':
          errorMessage = "Incorrect password. Please try again.";
          break;
        case 'auth/invalid-credential':
          errorMessage = "Invalid email or password.";
          break;
        case 'auth/too-many-requests':
          errorMessage = "Too many failed attempts. Please try again later.";
          break;
        case 'auth/network-request-failed':
          errorMessage = "Network error. Please check your internet connection.";
          break;
        default:
          errorMessage = error.message || "Authentication failed.";
      }
      
      setAuthError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleSignUp = async (e: React.FormEvent) => {
    e.preventDefault();
    setAuthError(null);
    
    if (!email || !password || password.length < 6) {
      setAuthError("Password must be at least 6 characters long.");
      return;
    }

    setLoading(true);

    try {
      const userCredential = await createUserWithEmailAndPassword(auth, email, password);
      const user = userCredential.user;
      
      const syncResult = await syncUserToMongoDB(
        user.uid,
        email.split('@')[0],
        email,
        "/default-avatar.png"
      );
      
      localStorage.setItem("userName", syncResult.name);
      localStorage.setItem("userEmail", email);
      localStorage.setItem("userPhoto", syncResult.image);
      localStorage.setItem("userUID", user.uid);
      localStorage.setItem("accountNumber", syncResult.accountId);
      
      await processLoginSuccess(user);
      
    } catch (error: any) {
      console.error('Sign up error:', error);
      
      switch (error.code) {
        case 'auth/email-already-in-use':
          setAuthError("An account already exists with this email. Please login instead.");
          break;
        case 'auth/invalid-email':
          setAuthError("Invalid email format.");
          break;
        case 'auth/operation-not-allowed':
          setAuthError("Email/password sign up is not enabled. Please contact support.");
          break;
        case 'auth/weak-password':
          setAuthError("Password should be at least 6 characters.");
          break;
        default:
          setAuthError(error.message || "Failed to create account.");
      }
    } finally {
      setLoading(false);
    }
  };

  const isValidEmail = (emailStr: string) => {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(emailStr);
  };


  // Loading state while checking if new user
  if (checkingNewUser) {
    return (
      <div className="min-h-screen bg-white flex items-center justify-center">
        <div className="text-center">
          <svg className="animate-spin h-10 w-10 text-blue-600 mx-auto mb-4" viewBox="0 0 24 24">
            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
          </svg>
          <p className="text-gray-600 text-sm">Setting up your account...</p>
        </div>
      </div>
    )
  }

  // Country Selection Page
  if (showCountryPage && pendingUserData) {
    return (
      <CountrySelectionPage 
        userData={pendingUserData}
        selectedGender={pendingGender}
        onComplete={handleGenderComplete} 
      />
    );
  }

  // Gender Selection Page
  if (showGenderPage && pendingUserData) {
    return (
      <GenderSelectionPage 
        userData={pendingUserData} 
        onContinue={handleGenderContinue} 
      />
    );
  }


  // Privacy Policy Page
  if (showPrivacyPolicy) {
    return (
      <div className="min-h-screen bg-white flex flex-col">
        <div className="flex items-center p-4 border-b border-gray-100">
          <button
            onClick={() => setShowPrivacyPolicy(false)}
            className="p-2 hover:bg-gray-100 rounded-full transition-colors cursor-pointer"
          >
            <ArrowLeft size={24} className="text-gray-700" />
          </button>
          <h1 className="text-lg font-semibold text-gray-900 ml-3">Privacy Policy</h1>
        </div>
        <div className="flex-1 p-6 overflow-y-auto">
          <div className="max-w-2xl mx-auto space-y-6 text-gray-700">
            <h2 className="text-2xl font-bold text-gray-900">Privacy Policy</h2>
            <p className="text-sm text-gray-500">Last updated: {new Date().toLocaleDateString()}</p>
            
            <section>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">1. Information We Collect</h3>
              <p>We collect information you provide directly to us, including your name, email address, and profile information when you create an account. We also collect information about your use of our services.</p>
            </section>

            <section>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">2. How We Use Your Information</h3>
              <p>We use the information we collect to provide, maintain, and improve our services, to communicate with you, and to develop new features.</p>
            </section>

            <section>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">3. Information Sharing</h3>
              <p>We do not share your personal information with third parties except as described in this privacy policy or with your consent.</p>
            </section>

            <section>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">4. Data Security</h3>
              <p>We take reasonable measures to help protect your personal information from loss, theft, misuse, and unauthorized access.</p>
            </section>

            <section>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">5. Your Choices</h3>
              <p>You can access, update, or delete your account information at any time through your account settings. You may also opt out of receiving promotional communications from us.</p>
            </section>

            <section>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">6. Contact Us</h3>
              <p>If you have any questions about this Privacy Policy, please contact us at support@hawaapp.com.</p>
            </section>
          </div>
        </div>
      </div>
    );
  }

  // Login Page
  if (showLoginPage) {
    return (
      <div className="min-h-screen relative flex flex-col bg-gray-900">
        <video 
                        controls={false}
              disablePictureInPicture
              disableRemotePlayback
              autoPlay 
          loop 
          playsInline
          muted
          preload="auto"
          poster="/video-thumbnail.jpg"
          onLoadedData={() => setVideoLoaded(true)}
          className={`absolute inset-0 w-full h-full object-cover z-0 transition-opacity duration-300 ${videoLoaded ? 'opacity-100' : 'opacity-0'}`}
        >
          <source src="/VID_20260804_011114_027_bsl.mp4" type="video/mp4" />
          Your browser does not support the video tag.
        </video>

        <div className="absolute inset-0 bg-black/40 z-0"></div>

        <div className="relative z-10 flex flex-col min-h-screen">
          <div className="flex items-center p-4">
            <button
              onClick={() => {
                setShowLoginPage(false);
                setEmail('');
                setPassword('');
                setAuthError(null);
              }}
              className="p-2 hover:bg-white/20 rounded-full transition-colors cursor-pointer"
            >
              <ArrowLeft size={24} className="text-white" />
            </button>
          </div>

          <div className="flex-1 flex flex-col items-center justify-start px-6 pt-8">
            <div className="w-full max-w-sm">
              <div className="text-center mb-6">
                <img 
                  src="/logo.png" 
                  alt="Hurry"
                  className="w-16 h-16 rounded-2xl mx-auto mb-3 drop-shadow-lg" 
                />
                <h1 className="text-2xl font-bold text-white">Welcome Back!</h1>
                <p className="text-white/70 text-sm mt-1">Sign in to your account</p>
              </div>

              <form onSubmit={handleEmailAuth} className="space-y-4">
                {authError && (
                  <div className="bg-red-500/20 backdrop-blur-md border border-red-300/30 text-white px-4 py-3 rounded-xl text-sm">
                    {authError}
                  </div>
                )}

                <div>
                  <label className="block text-sm font-medium text-white/90 mb-2">
                    E-mail Address
                  </label>
                  <input
                    type="email"
                    placeholder="Enter your email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full px-4 py-3 bg-white/10 backdrop-blur-md border border-white/20 rounded-xl focus:outline-none focus:border-white/50 transition-colors text-white placeholder-white/50"
                    required
                  />
                  {email && !isValidEmail(email) && (
                    <p className="text-xs text-red-300 mt-1">Please enter a valid email</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-white/90 mb-2">
                    Password
                  </label>
                  <div className="relative">
                    <input
                      type={showPassword ? "text" : "password"}
                      placeholder="Enter your password"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="w-full px-4 py-3 bg-white/10 backdrop-blur-md border border-white/20 rounded-xl focus:outline-none focus:border-white/50 transition-colors text-white placeholder-white/50 pr-12"
                      required
                      minLength={6}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-white/60 hover:text-white transition-colors cursor-pointer"
                    >
                      {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                    </button>
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading || !email || !password || !isValidEmail(email)}
                  className="w-full bg-white text-blue-600 font-semibold py-3 rounded-xl transition-all hover:bg-white/90 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer shadow-lg shadow-black/20 text-base"
                >
                  {loading ? (
                    <span className="flex items-center justify-center gap-2">
                      <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                      </svg>
                      Signing In...
                    </span>
                  ) : (
                    'Login'
                  )}
                </button>

                <div className="text-center">
                  <p className="text-sm text-white/70">
                    Don't have an account?{' '}
                    <button
                      type="button"
                      onClick={() => {
                        setShowLoginPage(false);
                        setShowSignUpPage(true);
                        setEmail('');
                        setPassword('');
                        setAuthError(null);
                      }}
                      className="text-blue-300 hover:text-blue-200 font-semibold cursor-pointer"
                    >
                      Sign Up
                    </button>
                  </p>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Sign Up Page
  if (showSignUpPage) {
    return (
      <div className="min-h-screen relative flex flex-col bg-gray-900">
        <video 
                        controls={false}
              disablePictureInPicture
              disableRemotePlayback
              autoPlay 
          loop 
          playsInline
          muted
          preload="auto"
          poster="/video-thumbnail.jpg"
          onLoadedData={() => setVideoLoaded(true)}
          className={`absolute inset-0 w-full h-full object-cover z-0 transition-opacity duration-300 ${videoLoaded ? 'opacity-100' : 'opacity-0'}`}
        >
          <source src="/VID_20260804_011114_027_bsl.mp4" type="video/mp4" />
          Your browser does not support the video tag.
        </video>

        <div className="absolute inset-0 bg-black/40 z-0"></div>

        <div className="relative z-10 flex flex-col min-h-screen">
          <div className="flex items-center p-4">
            <button
              onClick={() => {
                setShowSignUpPage(false);
                setShowLoginPage(true);
                setEmail('');
                setPassword('');
                setAuthError(null);
              }}
              className="p-2 hover:bg-white/20 rounded-full transition-colors cursor-pointer"
            >
              <ArrowLeft size={24} className="text-white" />
            </button>
          </div>

          <div className="flex-1 flex flex-col items-center justify-start px-6 pt-8">
            <div className="w-full max-w-sm">
              <div className="text-center mb-6">
                <img 
                  src="/logo.png" 
                  alt="Hurry"
                  className="w-16 h-16 rounded-2xl mx-auto mb-3 drop-shadow-lg" 
                />
                <h1 className="text-2xl font-bold text-white">Create Account</h1>
                <p className="text-white/70 text-sm mt-1">Sign up to get started</p>
              </div>

              <form onSubmit={handleSignUp} className="space-y-4">
                {authError && (
                  <div className="bg-red-500/20 backdrop-blur-md border border-red-300/30 text-white px-4 py-3 rounded-xl text-sm">
                    {authError}
                  </div>
                )}

                <div>
                  <label className="block text-sm font-medium text-white/90 mb-2">
                    E-mail Address
                  </label>
                  <input
                    type="email"
                    placeholder="Enter your email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full px-4 py-3 bg-white/10 backdrop-blur-md border border-white/20 rounded-xl focus:outline-none focus:border-white/50 transition-colors text-white placeholder-white/50"
                    required
                  />
                  {email && !isValidEmail(email) && (
                    <p className="text-xs text-red-300 mt-1">Please enter a valid email</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-white/90 mb-2">
                    Password
                  </label>
                  <div className="relative">
                    <input
                      type={showPassword ? "text" : "password"}
                      placeholder="Create a password (min 6 characters)"
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      className="w-full px-4 py-3 bg-white/10 backdrop-blur-md border border-white/20 rounded-xl focus:outline-none focus:border-white/50 transition-colors text-white placeholder-white/50 pr-12"
                      required
                      minLength={6}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 p-1 text-white/60 hover:text-white transition-colors cursor-pointer"
                    >
                      {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                    </button>
                  </div>
                </div>

                <button
                  type="submit"
                  disabled={loading || !email || !password || !isValidEmail(email)}
                  className="w-full bg-white text-blue-600 font-semibold py-3 rounded-xl transition-all hover:bg-white/90 disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer shadow-lg shadow-black/20 text-base"
                >
                  {loading ? (
                    <span className="flex items-center justify-center gap-2">
                      <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                      </svg>
                      Creating Account...
                    </span>
                  ) : (
                    'Sign Up'
                  )}
                </button>

                <div className="text-center">
                  <p className="text-sm text-white/70">
                    Already have an account?{' '}
                    <button
                      type="button"
                      onClick={() => {
                        setShowSignUpPage(false);
                        setShowLoginPage(true);
                        setEmail('');
                        setPassword('');
                        setAuthError(null);
                      }}
                      className="text-blue-300 hover:text-blue-200 font-semibold cursor-pointer"
                    >
                      Login
                    </button>
                  </p>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Main Landing Page
  return (
    <div className="min-h-screen relative flex flex-col items-center justify-between px-4 overflow-hidden bg-gray-900">
      
      <video 
                      controls={false}
              disablePictureInPicture
              disableRemotePlayback
              autoPlay 
        loop 
        playsInline
        muted
        preload="auto"
        poster="/video-thumbnail.jpg"
        onLoadedData={() => setVideoLoaded(true)}
        className={`absolute inset-0 w-full h-full object-cover z-0 transition-opacity duration-300 ${videoLoaded ? 'opacity-100' : 'opacity-0'}`}
      >
        <source src="/VID_20260804_011114_027_bsl.mp4" type="video/mp4" />
        Your browser does not support the video tag.
      </video>

      <div className="absolute inset-0 bg-black/30 z-0"></div>

      <div className="relative z-10 w-full flex flex-col items-center justify-between min-h-screen">
        

        {/* Logo pushed down */}
        <div className="flex flex-col items-center" style={{ marginTop: '16vh' }}>
          <div className="mb-0.5">
            <img 
              src="/logo.png" 
              alt="Hurry"
              className="w-20 h-20 rounded-2xl drop-shadow-lg" 
            />
          </div>
          <h1 className="text-3xl font-bold text-white tracking-wide drop-shadow-lg">Hurry</h1>
        </div>

        <div style={{ marginTop: '14vh' }}></div>

        {/* Buttons pushed down - 3D effect removed, flat design */}
        <div className="w-full flex flex-col items-center gap-4 mb-2">
          <button
            onClick={handleGoogleLogin}
            disabled={loading}
            className="w-70 bg-white rounded-full p-3.5 flex items-center justify-center gap-3 transition-all hover:scale-[1.02] active:scale-95 cursor-pointer disabled:opacity-40 disabled:cursor-not-allowed shadow-md"
          >
            {loading ? (
              <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
              </svg>
            ) : (
              <>
                <svg width="22" height="22" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                  <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.06 5.06 0 0 1-2.2 3.32v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.1z" fill="#4285F4"/>
                  <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
                  <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#FBBC05"/>
                  <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#EA4335"/>
                </svg>
                <span className="font-semibold text-gray-800 text-base">
                  Continue with Google
                </span>
              </>
            )}
          </button>

          <button
            onClick={handleLoginWithAccountClick}
            className="w-70 rounded-full p-3.5 flex items-center justify-center gap-3 transition-all hover:scale-[1.02] active:scale-95 cursor-pointer text-white bg-blue-600 hover:bg-blue-700 shadow-md"
          >
            <User size={22} />
            <span className="font-semibold text-base">
              Login with Account
            </span>
          </button>
        </div>

        <div className="w-full max-w-sm text-center pb-4">
          <p className="text-xs text-white/80 drop-shadow">
            Login means you agree to the{' '}
            <span className="text-white font-medium">Terms of Service</span>
            {', '}
            <button
              type="button"
              onClick={() => setShowPrivacyPolicy(true)}
              className="text-blue-300 hover:text-blue-200 font-medium cursor-pointer"
            >
              Privacy Policy
            </button>
          </p>
        </div>
      </div>

      {/* BAN NOTIFICATION CARD - auto closes in 3s, no close button */}
      {banMessage && (
        <div className="fixed bottom-[10vh] left-1/2 -translate-x-1/2 bg-black/90 backdrop-blur-sm text-white text-sm px-6 py-4 rounded-xl text-center shadow-lg z-50 whitespace-pre-wrap min-w-[280px] max-w-[90vw]">
          {banMessage}
        </div>
      )}
    </div>
  )
  }
