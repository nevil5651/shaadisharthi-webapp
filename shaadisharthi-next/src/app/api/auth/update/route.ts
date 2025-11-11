import { NextResponse } from 'next/server'
import { cookies } from 'next/headers'

export async function POST(request: Request) {
  const cookieStore = await cookies()
  const sessionCookie = cookieStore.get('session')?.value

  if (!sessionCookie) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }

  const baseUrl = process.env.NEXT_INTERNAL_API_URL ?? process.env.NEXT_PUBLIC_API_URL

  try {
    const updateData = await request.json()
    
    // Remove email if present (shouldn't be editable)
    if (updateData.email) {
      delete updateData.email
    }

    // Validate required fields
    if (!updateData.name || typeof updateData.name !== 'string') {
      return NextResponse.json(
        { error: 'Name is required' },
        { status: 400 }
      )
    }

    // Proper JSON payload for Java backend
    const jsonPayload = {
      name: updateData.name,
      phone_no: updateData.phone_no || null, // Send null instead of empty string
      address: updateData.address || null
    }

    // Call your Java backend with proper JSON
    const backendResponse = await fetch(`${baseUrl}/Customer/cstmr-acc`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Cookie': `session=${sessionCookie}`
      },
      body: JSON.stringify(jsonPayload)
    })

    if (!backendResponse.ok) {
      const errorData = await backendResponse.json()
      return NextResponse.json(
        { error: errorData.error || 'Failed to update profile' },
        { status: backendResponse.status }
      )
    }

    const responseData = await backendResponse.json()

    const updatedUser = responseData.data || responseData
   return NextResponse.json(updatedUser)

   
  } catch (error) {
    console.error('Update profile error:', error)
    return NextResponse.json(
      { error: 'Internal server error' },
      { status: 500 }
    )
  }
}