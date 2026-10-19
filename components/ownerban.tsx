'use client'

import React, { useState, useEffect } from 'react'
import { MoreVertical, X, ChevronDown } from 'lucide-react'
import { apiUrl } from "@/src/lib/api"

export default function OwnerBan() {
  const [bans, setBans] = useState<any[]>([])
  const [searchId, setSearchId] = useState('')
  const [searching, setSearching] = useState(false)

  // Ban form state
  const [isBanFormOpen, setIsBanFormOpen] = useState(false)
  const [targetUser, setTargetUser] = useState<any>(null)

  // Detail modal state
  const [selectedBan, setSelectedBan] = useState<any>(null)
  const [isDetailModalOpen, setIsDetailModalOpen] = useState(false)
  const [isUnbanConfirmOpen, setIsUnbanConfirmOpen] = useState(false)
  const [unbanReason, setUnbanReason] = useState('')

  // Custom Dropdown State (To control which list is open)
  const [openDropdown, setOpenDropdown] = useState('')

  // Ban form fields
  const [inlineUserId, setInlineUserId] = useState('') 
  const [description, setDescription] = useState('')
  const [banType, setBanType] = useState('Illegal')
  const [banBy, setBanBy] = useState('Official staff')
  const [memberName, setMemberName] = useState('') 
  const [timeOption, setTimeOption] = useState('2Hours')
  const [customTime, setCustomTime] = useState('')
  const [unbanTimeStr, setUnbanTimeStr] = useState('')

  useEffect(() => {
    fetchBans()
  }, [])

  useEffect(() => {
    calculateUnbanTime()
  }, [timeOption, customTime])

  const fetchBans = async () => {
    try {
      const res = await fetch(apiUrl('/api/bans'))
      if (res.ok) {
        const data = await res.json()
        setBans(data.bans || [])
      }
    } catch (e) {
      console.error(e)
    }
  }

  const handleSearch = async () => {
    const trimmedId = searchId.trim()
    if (!trimmedId) return
    setSearching(true)
    try {
      const res = await fetch(apiUrl(`/api/users?search=${encodeURIComponent(trimmedId)}`))
      if (res.ok) {
        const data = await res.json()
        if (data.users && data.users.length > 0) {
          setTargetUser(data.users[0])
          setIsBanFormOpen(true)
          resetBanForm()
        } else {
          alert('User not found')
        }
      } else {
        alert('User not found')
      }
    } catch (e) {
      console.error(e)
      alert("Error searching user")
    }
    setSearching(false)
  }

  const resetBanForm = () => {
    setDescription('')
    setBanType('Illegal')
    setBanBy('Official staff')
    setMemberName('')
    setTimeOption('2Hours')
    setCustomTime('')
    setOpenDropdown('')
  }

  const calculateUnbanTime = () => {
    if (timeOption === 'Permanent' || timeOption === 'Device Ban') {
      setUnbanTimeStr('Never')
      return
    }

    const now = new Date()
    let msToAdd = 0

    if (timeOption === '2Hours') msToAdd = 2 * 60 * 60 * 1000
    else if (timeOption === '24Hours') msToAdd = 24 * 60 * 60 * 1000
    else if (timeOption === '7 Days') msToAdd = 7 * 24 * 60 * 60 * 1000
    else if (timeOption === 'Custom') {
      const hours = parseInt(customTime) || 0
      msToAdd = hours * 60 * 60 * 1000
    }

    const unbanDate = new Date(now.getTime() + msToAdd)
    setUnbanTimeStr(unbanDate.toLocaleString())
  }

  const submitInlineBan = async () => {
    const trimmedId = inlineUserId.trim();
    if (!trimmedId) {
      alert("Please enter User ID");
      return;
    }

    const now = new Date()
    let unbanTimestamp = -1

    if (timeOption !== 'Permanent' && timeOption !== 'Device Ban') {
      let msToAdd = 0
      if (timeOption === '2Hours') msToAdd = 2 * 60 * 60 * 1000
      else if (timeOption === '24Hours') msToAdd = 24 * 60 * 60 * 1000
      else if (timeOption === '7 Days') msToAdd = 7 * 24 * 60 * 60 * 1000
      else if (timeOption === 'Custom') {
        const hours = parseInt(customTime) || 0
        msToAdd = hours * 60 * 60 * 1000
      }
      unbanTimestamp = now.getTime() + msToAdd
    }

    try {
      const resUser = await fetch(apiUrl(`/api/users?search=${encodeURIComponent(trimmedId)}`))
      if (resUser.ok) {
        const data = await resUser.json()
        if (data.users && data.users.length > 0) {
          const tUser = data.users[0]
          const payload = {
            accountId: tUser.accountId,
            userId: tUser.id || tUser.uid,
            userName: tUser.name,
            userImage: tUser.image || tUser.avatar,
            type: banType,
            banBy: banBy,
            memberName: memberName,
            timeOption: timeOption,
            customTime: customTime,
            banTime: now.getTime(),
            unbanTime: unbanTimestamp,
            description: description,
            ipAddress: tUser.lastIp || '',
            deviceId: tUser.lastDeviceId || ''
          }
          const resBan = await fetch(apiUrl('/api/bans'), {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', 'x-requester-id': '100003' },
            body: JSON.stringify(payload)
          })
          if (resBan.ok) {
            setInlineUserId('')
            setDescription('')
            setMemberName('')
            fetchBans()
            alert("Banned successfully")
          } else {
            alert("Failed to ban user")
          }
        } else {
          alert('User not found')
        }
      } else {
        alert('User not found')
      }
    } catch (e) {
      console.error(e)
      alert("Error searching user")
    }
  }

  const submitBan = async () => {
    if (!targetUser) return
    const now = new Date()
    let unbanTimestamp = -1

    if (timeOption !== 'Permanent' && timeOption !== 'Device Ban') {
      let msToAdd = 0
      if (timeOption === '2Hours') msToAdd = 2 * 60 * 60 * 1000
      else if (timeOption === '24Hours') msToAdd = 24 * 60 * 60 * 1000
      else if (timeOption === '7 Days') msToAdd = 7 * 24 * 60 * 60 * 1000
      else if (timeOption === 'Custom') {
        const hours = parseInt(customTime) || 0
        msToAdd = hours * 60 * 60 * 1000
      }
      unbanTimestamp = now.getTime() + msToAdd
    }

    const payload = {
      accountId: targetUser.accountId,
      userId: targetUser.id || targetUser.uid,
      userName: targetUser.name,
      userImage: targetUser.image || targetUser.avatar,
      type: banType,
      banBy: banBy,
      memberName: memberName,
      timeOption: timeOption,
      customTime: customTime,
      banTime: now.getTime(),
      unbanTime: unbanTimestamp,
      description: description,
      ipAddress: targetUser.lastIp || '',
      deviceId: targetUser.lastDeviceId || ''
    }

    try {
      const res = await fetch(apiUrl('/api/bans'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-requester-id': '100003' },
        body: JSON.stringify(payload)
      })
      if (res.ok) {
        setIsBanFormOpen(false)
        fetchBans()
      } else {
        alert("Failed to ban user")
      }
    } catch (e) {
      console.error(e)
      alert("Error banning user")
    }
  }

  const submitUnban = async () => {
    if (!selectedBan || !unbanReason.trim()) {
      alert("Please enter a reason for unbanning")
      return
    }

    try {
      const res = await fetch(apiUrl('/api/bans/unban'), {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-requester-id': '100003' },
        body: JSON.stringify({ accountId: selectedBan.accountId, reason: unbanReason })
      })
      if (res.ok) {
        setIsUnbanConfirmOpen(false)
        setIsDetailModalOpen(false)
        setUnbanReason('')
        fetchBans()
      }
    } catch (e) {
      console.error(e)
    }
  }

  return (
    <div className="p-4 md:p-8 max-w-7xl mx-auto w-full text-slate-900 dark:text-slate-100">
      <h2 className="text-2xl font-bold mb-4">Ban</h2>

      {/* NEW ON-SCREEN BAN FORM */}
      <div className="mb-8 max-w-sm space-y-4">
        
        <div>
          <div className="font-semibold mb-1">User ID</div>
          <input
            type="text"
            value={inlineUserId}
            onChange={(e) => setInlineUserId(e.target.value)}
            className="w-full p-2 bg-slate-100 dark:bg-slate-800 rounded-md shadow-none border-none outline-none"
            placeholder="Enter ID"
          />
        </div>

        <div>
          <div className="font-semibold mb-1">Description</div>
          <input
            type="text"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            className="w-full p-2 bg-slate-100 dark:bg-slate-800 rounded-md shadow-none border-none outline-none"
            placeholder="Reason of ban"
          />
        </div>

        {/* CUSTOM DROPDOWN: Member */}
        <div>
          <div className="font-semibold mb-1">Member</div>
          <div className="relative">
            <div 
              onClick={() => setOpenDropdown(openDropdown === 'inline-member' ? '' : 'inline-member')}
              className="w-full p-2 bg-slate-100 dark:bg-slate-800 rounded-md flex justify-between items-center cursor-pointer select-none"
            >
              <span>{banBy}</span>
              <ChevronDown className="w-4 h-4 text-slate-500" />
            </div>
            {openDropdown === 'inline-member' && (
              <div className="absolute top-full left-0 w-full mt-1 bg-slate-200 dark:bg-slate-700 rounded-md z-50 overflow-hidden">
                {['Head', 'Official staff'].map(opt => (
                  <div 
                    key={opt} 
                    onClick={() => { setBanBy(opt); setOpenDropdown(''); }}
                    className="p-2 hover:bg-slate-300 dark:hover:bg-slate-600 cursor-pointer"
                  >
                    {opt}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Name of the member Input */}
        <div>
          <div className="font-semibold mb-1">Name of the member</div>
          <input
            type="text"
            value={memberName}
            onChange={(e) => setMemberName(e.target.value)}
            className="w-full p-2 bg-slate-100 dark:bg-slate-800 rounded-md shadow-none border-none outline-none"
            placeholder="Enter member name"
          />
        </div>

        {/* CUSTOM DROPDOWN: Types */}
        <div>
          <div className="font-semibold mb-1">Types</div>
          <div className="relative">
            <div 
              onClick={() => setOpenDropdown(openDropdown === 'inline-types' ? '' : 'inline-types')}
              className="w-full p-2 bg-slate-100 dark:bg-slate-800 rounded-md flex justify-between items-center cursor-pointer select-none"
            >
              <span>{banType}</span>
              <ChevronDown className="w-4 h-4 text-slate-500" />
            </div>
            {openDropdown === 'inline-types' && (
              <div className="absolute top-full left-0 w-full mt-1 bg-slate-200 dark:bg-slate-700 rounded-md z-50 overflow-hidden">
                {['Illegal', 'Violence', 'Fraud', 'Abusing', 'Fake official', 'Others'].map(opt => (
                  <div 
                    key={opt} 
                    onClick={() => { setBanType(opt); setOpenDropdown(''); }}
                    className="p-2 hover:bg-slate-300 dark:hover:bg-slate-600 cursor-pointer"
                  >
                    {opt}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* CUSTOM DROPDOWN: Time */}
        <div>
          <div className="font-semibold mb-1">Time</div>
          <div className="relative">
            <div 
              onClick={() => setOpenDropdown(openDropdown === 'inline-time' ? '' : 'inline-time')}
              className="w-full p-2 bg-slate-100 dark:bg-slate-800 rounded-md flex justify-between items-center cursor-pointer select-none"
            >
              <span>{timeOption}</span>
              <ChevronDown className="w-4 h-4 text-slate-500" />
            </div>
            {openDropdown === 'inline-time' && (
              <div className="absolute top-full left-0 w-full mt-1 bg-slate-200 dark:bg-slate-700 rounded-md z-50 overflow-hidden">
                {['2Hours', '24Hours', '7 Days', 'Custom', 'Permanent', 'Device Ban'].map(opt => (
                  <div 
                    key={opt} 
                    onClick={() => { setTimeOption(opt); setOpenDropdown(''); }}
                    className="p-2 hover:bg-slate-300 dark:hover:bg-slate-600 cursor-pointer"
                  >
                    {opt}
                  </div>
                ))}
              </div>
            )}
          </div>
          {/* Custom Time Input */}
          {timeOption === 'Custom' && (
            <input
              type="number"
              placeholder="Enter hours"
              value={customTime}
              onChange={(e) => setCustomTime(e.target.value)}
              className="w-full p-2 mt-2 bg-slate-100 dark:bg-slate-800 rounded-md shadow-none border-none outline-none"
            />
          )}
        </div>

        <div>
          <div className="font-semibold mb-1">Unban Time</div>
          <div className="w-full p-2 bg-slate-100 dark:bg-slate-800 rounded-md shadow-none border-none outline-none text-slate-500">
            {unbanTimeStr || 'N/A'}
          </div>
        </div>

        <button
          onClick={submitInlineBan}
          className="w-full bg-blue-600 text-white font-bold py-3 rounded-md mt-4 shadow-none border-none"
        >
          Ban
        </button>
      </div>

      {/* SEARCH INPUT BAR - Edge to edge w-full */}
      <div className="flex gap-2 mb-8 items-center w-full border-t border-slate-200 dark:border-slate-800 pt-6">
        <input
          type="text"
          placeholder="Search by ID Number (Optional)"
          value={searchId}
          onChange={(e) => setSearchId(e.target.value)}
          className="flex-1 p-2 border border-slate-300 dark:border-slate-700 rounded-lg bg-white dark:bg-slate-900 text-slate-900 dark:text-white"
        />
        <button
          onClick={handleSearch}
          disabled={searching}
          className="p-2 bg-blue-600 text-white rounded-lg px-4 whitespace-nowrap"
        >
          {searching ? '...' : 'Search'}
        </button>
      </div>

      <h3 className="text-lg font-semibold mb-4">Banned Users</h3>

      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse">
          <thead>
            <tr className="border-b border-slate-200 dark:border-slate-800">
              <th className="p-2 font-semibold">S.No</th>
              <th className="p-2 font-semibold">User Dp</th>
              <th className="p-2 font-semibold">ID</th>
              <th className="p-2 font-semibold">Type</th>
              <th className="p-2 font-semibold">Ban Time</th>
              <th className="p-2 font-semibold">Unban Time</th>
              <th className="p-2 font-semibold">Date/Time</th>
              <th className="p-2 font-semibold">Ban By</th>
              <th className="p-2 font-semibold"></th>
            </tr>
          </thead>
          <tbody>
            {bans.map((ban, index) => (
              <tr key={index} className="border-b border-slate-100 dark:border-slate-800/50 hover:bg-slate-50 dark:hover:bg-slate-800/20">
                <td className="p-2">{index + 1}</td>
                <td className="p-2">
                  <img src={ban.userImage || '/default-avatar.png'} alt="dp" className="w-8 h-8 rounded-full object-cover" />
                </td>
                <td className="p-2">{ban.accountId}</td>
                <td className="p-2">{ban.type}</td>
                <td className="p-2">{ban.timeOption}</td>
                <td className="p-2">
                  {ban.unbanTime === -1 ? 'Never' : new Date(ban.unbanTime).toLocaleString()}
                </td>
                <td className="p-2">{new Date(ban.banTime).toLocaleString()}</td>
                <td className="p-2">{ban.banBy}</td>
                <td className="p-2">
                  <button onClick={() => { setSelectedBan(ban); setIsDetailModalOpen(true); }} className="p-1 hover:bg-slate-200 dark:hover:bg-slate-700 rounded-full">
                    <MoreVertical className="w-5 h-5 text-slate-500" />
                  </button>
                </td>
              </tr>
            ))}
            {bans.length === 0 && (
              <tr>
                <td colSpan={9} className="p-4 text-center text-slate-500">No banned accounts found</td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* OLD BAN FORM MODAL (Same custom dropdown logic applied here too) */}
      {isBanFormOpen && targetUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white dark:bg-slate-900 rounded-md w-full max-w-md p-6 overflow-y-auto max-h-[90vh] text-slate-900 dark:text-white shadow-none border-none relative">
            
            <button onClick={() => setIsBanFormOpen(false)} className="absolute top-4 right-4 text-slate-500 hover:text-slate-800 dark:hover:text-white">
              <X className="w-6 h-6" />
            </button>
            
            <h2 className="text-xl font-bold mb-6 text-center">Add</h2>

            <div className="flex flex-col items-center gap-2 mb-6">
              <img src={targetUser.image || targetUser.avatar || '/default-avatar.png'} alt="user" className="w-16 h-16 rounded-full object-cover" />
              <div className="font-semibold">{targetUser.name || 'Unknown'}</div>
            </div>

            <div className="space-y-4">
              <div>
                <div className="font-semibold mb-1">User ID</div>
                <input 
                  type="text" 
                  value={targetUser.accountId} 
                  readOnly 
                  className="w-full p-2 bg-slate-100 dark:bg-slate-800 rounded-md shadow-none border-none outline-none" 
                />
              </div>

              <div>
                <div className="font-semibold mb-1">Description</div>
                <input 
                  type="text" 
                  value={description} 
                  onChange={(e) => setDescription(e.target.value)} 
                  className="w-full p-2 bg-slate-100 dark:bg-slate-800 rounded-md shadow-none border-none outline-none" 
                  placeholder="Reason of ban" 
                />
              </div>

              {/* Modal CUSTOM DROPDOWN: Member */}
              <div>
                <div className="font-semibold mb-1">Member</div>
                <div className="relative">
                  <div 
                    onClick={() => setOpenDropdown(openDropdown === 'modal-member' ? '' : 'modal-member')}
                    className="w-full p-2 bg-slate-100 dark:bg-slate-800 rounded-md flex justify-between items-center cursor-pointer select-none"
                  >
                    <span>{banBy}</span>
                    <ChevronDown className="w-4 h-4 text-slate-500" />
                  </div>
                  {openDropdown === 'modal-member' && (
                    <div className="absolute top-full left-0 w-full mt-1 bg-slate-200 dark:bg-slate-700 rounded-md z-50 overflow-hidden">
                      {['Head', 'Official staff'].map(opt => (
                        <div 
                          key={opt} 
                          onClick={() => { setBanBy(opt); setOpenDropdown(''); }}
                          className="p-2 hover:bg-slate-300 dark:hover:bg-slate-600 cursor-pointer"
                        >
                          {opt}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              <div>
                <div className="font-semibold mb-1">Name of the member</div>
                <input
                  type="text"
                  value={memberName}
                  onChange={(e) => setMemberName(e.target.value)}
                  className="w-full p-2 bg-slate-100 dark:bg-slate-800 rounded-md shadow-none border-none outline-none"
                  placeholder="Enter member name"
                />
              </div>

              {/* Modal CUSTOM DROPDOWN: Types */}
              <div>
                <div className="font-semibold mb-1">Types</div>
                <div className="relative">
                  <div 
                    onClick={() => setOpenDropdown(openDropdown === 'modal-types' ? '' : 'modal-types')}
                    className="w-full p-2 bg-slate-100 dark:bg-slate-800 rounded-md flex justify-between items-center cursor-pointer select-none"
                  >
                    <span>{banType}</span>
                    <ChevronDown className="w-4 h-4 text-slate-500" />
                  </div>
                  {openDropdown === 'modal-types' && (
                    <div className="absolute top-full left-0 w-full mt-1 bg-slate-200 dark:bg-slate-700 rounded-md z-50 overflow-hidden">
                      {['Illegal', 'Violence', 'Fraud', 'Abusing', 'Fake official', 'Others'].map(opt => (
                        <div 
                          key={opt} 
                          onClick={() => { setBanType(opt); setOpenDropdown(''); }}
                          className="p-2 hover:bg-slate-300 dark:hover:bg-slate-600 cursor-pointer"
                        >
                          {opt}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>

              {/* Modal CUSTOM DROPDOWN: Time */}
              <div>
                <div className="font-semibold mb-1">Time</div>
                <div className="relative">
                  <div 
                    onClick={() => setOpenDropdown(openDropdown === 'modal-time' ? '' : 'modal-time')}
                    className="w-full p-2 bg-slate-100 dark:bg-slate-800 rounded-md flex justify-between items-center cursor-pointer select-none"
                  >
                    <span>{timeOption}</span>
                    <ChevronDown className="w-4 h-4 text-slate-500" />
                  </div>
                  {openDropdown === 'modal-time' && (
                    <div className="absolute top-full left-0 w-full mt-1 bg-slate-200 dark:bg-slate-700 rounded-md z-50 overflow-hidden">
                      {['2Hours', '24Hours', '7 Days', 'Custom', 'Permanent', 'Device Ban'].map(opt => (
                        <div 
                          key={opt} 
                          onClick={() => { setTimeOption(opt); setOpenDropdown(''); }}
                          className="p-2 hover:bg-slate-300 dark:hover:bg-slate-600 cursor-pointer"
                        >
                          {opt}
                        </div>
                      ))}
                    </div>
                  )}
                </div>
                {timeOption === 'Custom' && (
                  <input 
                    type="number" 
                    placeholder="Enter hours" 
                    value={customTime} 
                    onChange={(e) => setCustomTime(e.target.value)} 
                    className="w-full p-2 mt-2 bg-slate-100 dark:bg-slate-800 rounded-md shadow-none border-none outline-none" 
                  />
                )}
              </div>

              <div>
                <div className="font-semibold mb-1">Unban Time</div>
                <div className="w-full p-2 bg-slate-100 dark:bg-slate-800 rounded-md shadow-none border-none outline-none text-slate-500">
                  {unbanTimeStr || 'N/A'}
                </div>
              </div>

              <button 
                onClick={submitBan} 
                className="w-full bg-blue-600 text-white font-bold py-3 rounded-md mt-4 shadow-none border-none"
              >
                Ban
              </button>
            </div>
          </div>
        </div>
      )}

      {/* DETAIL MODAL */}
      {isDetailModalOpen && selectedBan && !isUnbanConfirmOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white dark:bg-slate-900 rounded-2xl w-full max-w-md p-6 text-slate-900 dark:text-white shadow-xl relative">
            <button onClick={() => setIsDetailModalOpen(false)} className="absolute top-4 right-4 text-slate-500 hover:text-slate-800 dark:hover:text-white">
              <X className="w-6 h-6" />
            </button>
            <h2 className="text-xl font-bold mb-4 text-center">Ban Details</h2>

            <div className="flex items-center gap-3 mb-4 p-3 bg-slate-50 dark:bg-slate-800 rounded-lg">
              <img src={selectedBan.userImage || '/default-avatar.png'} alt="user" className="w-12 h-12 rounded-full object-cover" />
              <div>
                <div className="font-bold">{selectedBan.userName || 'Unknown'}</div>
                <div className="text-sm text-slate-500">ID: {selectedBan.accountId}</div>
              </div>
            </div>

            <div className="space-y-3 text-sm">
              <div className="flex justify-between"><span className="text-slate-500">Type:</span> <span className="font-medium">{selectedBan.type}</span></div>
              <div className="flex justify-between"><span className="text-slate-500">Description:</span> <span className="font-medium text-right max-w-[200px]">{selectedBan.description || 'N/A'}</span></div>
              <div className="flex justify-between"><span className="text-slate-500">Ban By:</span> <span className="font-medium">{selectedBan.banBy} {selectedBan.memberName ? `(${selectedBan.memberName})` : ''}</span></div>
              <div className="flex justify-between"><span className="text-slate-500">Time Option:</span> <span className="font-medium">{selectedBan.timeOption} {selectedBan.customTime ? `(${selectedBan.customTime}h)` : ''}</span></div>
              <div className="flex justify-between"><span className="text-slate-500">Ban Date:</span> <span className="font-medium">{new Date(selectedBan.banTime).toLocaleString()}</span></div>
              <div className="flex justify-between"><span className="text-slate-500">Unban Date:</span> <span className="font-medium">{selectedBan.unbanTime === -1 ? 'Never' : new Date(selectedBan.unbanTime).toLocaleString()}</span></div>
            </div>

            <div className="mt-6 flex gap-3">
              <button onClick={() => setIsUnbanConfirmOpen(true)} className="flex-1 bg-green-600 text-white font-bold py-2 rounded-lg hover:bg-green-700">
                Unban
              </button>
              <button onClick={() => setIsDetailModalOpen(false)} className="flex-1 bg-slate-200 dark:bg-slate-800 font-bold py-2 rounded-lg hover:bg-slate-300 dark:hover:bg-slate-700">
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* UNBAN REASON MODAL */}
      {isUnbanConfirmOpen && selectedBan && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
          <div className="bg-white dark:bg-slate-900 rounded-2xl w-full max-w-sm p-6 text-slate-900 dark:text-white shadow-xl relative">
            <h2 className="text-lg font-bold mb-4 text-center">Unban Reason</h2>
            <p className="text-sm text-slate-500 mb-4 text-center">Please provide a reason to unban ID {selectedBan.accountId}</p>
            <input
              type="text"
              value={unbanReason}
              onChange={(e) => setUnbanReason(e.target.value)}
              className="w-full p-2 border rounded-lg dark:border-slate-700 bg-transparent mb-4"
              placeholder="Reason..."
            />
            <div className="flex gap-3">
              <button onClick={() => setIsUnbanConfirmOpen(false)} className="flex-1 bg-slate-200 dark:bg-slate-800 font-bold py-2 rounded-lg">Cancel</button>
              <button onClick={submitUnban} className="flex-1 bg-green-600 text-white font-bold py-2 rounded-lg hover:bg-green-700">Confirm Unban</button>
            </div>
          </div>
        </div>
      )}

    </div>
  )
}

