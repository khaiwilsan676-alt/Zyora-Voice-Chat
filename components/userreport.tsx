'use client';

import React, { useState, useRef } from 'react';
import { ArrowLeft, Plus, X } from 'lucide-react';
import { socket } from '../src/lib/socket';

export default function UserReportScreen({
  currentUser,
  targetUser,
  onClose
}: {
  currentUser?: any;
  targetUser?: any;
  onClose?: () => void;
}) {
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const [description, setDescription] = useState('');
  const [proofImage, setProofImage] = useState<string | null>(null);
  const [isSuccess, setIsSuccess] = useState(false);
  
  const fileInputRef = useRef<HTMLInputElement>(null);

  const categories = [
    { id: 'violence', label: 'Violence' },
    { id: 'abusing', label: 'Abusing' },
    { id: 'illegal', label: 'Illegal' },
    { id: 'others', label: "Other's" },
  ];

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setProofImage(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const removeImage = (e: React.MouseEvent) => {
    e.stopPropagation();
    setProofImage(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleSubmit = () => {
    if (!selectedCategory) {
      alert("Please select a report category!");
      return;
    }

    if (socket.connected) {
      const reportId = `report_${Date.now()}_${currentUser?.accountId || 'unknown'}`;
      socket.emit('user_report', {
        id: reportId,
        senderId: currentUser?.accountId || currentUser?.uid || '',
        senderName: currentUser?.name || 'User',
        senderPhoto: currentUser?.photo || currentUser?.image || '',
        reportedId: targetUser?.accountId || targetUser?.uid || '',
        reportedName: targetUser?.name || 'User',
        reportedPhoto: targetUser?.photo || targetUser?.image || '',
        category: selectedCategory,
        description,
        proofImage,
        timestamp: Date.now(),
      });
    }

    setIsSuccess(true);
    setTimeout(() => {
      if (onClose) onClose();
    }, 2000);
  };

  return (
    <div className="fixed inset-0 z-50 bg-[#f0f2f5] flex flex-col font-sans">
      
      {/* ----- Header Area ----- */}
      <div 
        className="relative pt-safe pb-4 flex items-center justify-center"
        style={{
          background: 'linear-gradient(to bottom, #3b82f6 0%, #f0f2f5 100%)',
          paddingTop: 'calc(env(safe-area-inset-top, 0px) + 8px)'
        }}
      >
        <button 
          onClick={onClose} 
          className="absolute left-2 top-auto p-2 hover:bg-white/20 rounded-full transition-colors"
        >
          <ArrowLeft size={24} className="text-gray-900" />
        </button>
        
        <h1 className="text-xl font-bold text-gray-900 tracking-wide">
          Report
        </h1>
      </div>

      {isSuccess ? (
        <div className="flex-1 flex flex-col items-center justify-center p-4">
          <div className="bg-green-50 border border-green-200 rounded-md p-8 text-center max-w-md w-full">
            <div className="text-4xl mb-4">✅</div>
            <h2 className="text-xl font-bold text-green-700 mb-2">Thank You!</h2>
            <p className="text-green-600">Your report has been submitted successfully.</p>
          </div>
        </div>
      ) : (
        <>
      {/* ----- Main Scrollable Content ----- */}
      <div className="flex-1 overflow-y-auto px-4 pb-6">
        
        {/* Category Cards (No borders, no shadows) */}
        <div className="mt-2 grid grid-cols-2 gap-3">
          {categories.map((cat) => {
            const isSelected = selectedCategory === cat.id;
            return (
              <button
                key={cat.id}
                onClick={() => setSelectedCategory(cat.id)}
                // Selection par sirf background color change hoga, border aur shadow hata diya bss
                className={`py-4 px-2 rounded-md text-sm font-semibold transition-all duration-200 ${
                  isSelected 
                    ? 'bg-blue-100 text-blue-600' 
                    : 'bg-white text-gray-700'
                }`}
              >
                {cat.label}
              </button>
            );
          })}
        </div>

        {/* Description Section (No borders, no shadows) */}
        <div className="mt-6">
          <h2 className="text-base font-bold text-gray-800 mb-2">Description</h2>
          <textarea
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Type your issue"
            // Border aur shadow dono hata diye bss
            className="w-full bg-white rounded-md p-3 text-sm text-gray-800 outline-none min-h-[120px] resize-none"
          />
        </div>

        {/* Attach Proof Section (Sirf dashed border rakha hai, shadow hata di) */}
        <div className="mt-6">
          <h2 className="text-base font-bold text-gray-800 mb-2">Attach Proof</h2>
          
          <input 
            type="file" 
            ref={fileInputRef} 
            onChange={handleImageUpload} 
            accept="image/*" 
            className="hidden" 
          />
          
          {/* Dashed border yahan same hai, shadow hata di bss */}
          <div 
            onClick={() => !proofImage && fileInputRef.current?.click()}
            className="w-24 h-24 bg-white border-2 border-dashed border-gray-300 rounded-md flex items-center justify-center cursor-pointer relative overflow-hidden hover:bg-gray-50 transition-colors"
          >
            {proofImage ? (
              <>
                <img src={proofImage} alt="Proof" className="w-full h-full object-cover" />
                <button 
                  onClick={removeImage}
                  className="absolute top-1 right-1 bg-black/50 p-1 rounded-full text-white hover:bg-black/70"
                >
                  <X size={14} />
                </button>
              </>
            ) : (
              <Plus size={32} className="text-gray-400" />
            )}
          </div>
        </div>
        
      </div>

      {/* ----- Bottom Submit Button (Width kam, rounded full) ----- */}
      <div className="p-4 bg-transparent flex justify-center">
        <button 
          onClick={handleSubmit}
          // rounded-full kiya, width kam karke max width 250px aur shadow hata di bss
          className="w-2/3 max-w-[250px] bg-blue-500 text-white font-bold text-lg py-3 rounded-full active:scale-[0.98] transition-transform"
        >
          Submit
        </button>
      </div>
        </>
      )}

    </div>
  );
}

