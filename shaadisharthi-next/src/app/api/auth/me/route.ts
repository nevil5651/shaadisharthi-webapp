import { NextResponse } from 'next/server'
import { cookies } from 'next/headers'

export async function GET() {
  const cookieStore = await cookies()
  const sessionCookie = cookieStore.get('session')?.value

  if (!sessionCookie) {
    return NextResponse.json({ error: 'Unauthorized' }, { status: 401 })
  }
  
  const baseUrl = process.env.NEXT_INTERNAL_API_URL ?? process.env.NEXT_PUBLIC_API_URL

  try {
    // Call your actual backend API
    const backendResponse = await fetch(`${baseUrl}/Customer/cstmr-acc`, {
      headers: {
        'Content-Type': 'application/json',
        'Cookie': `session=${sessionCookie}`
      },
      credentials: 'include'
    })

    if (!backendResponse.ok) {
      throw new Error('Failed to fetch account details')
    }

    const userData = await backendResponse.json()
    
    // Set cache headers for the client (5 minutes)
    const response = NextResponse.json(userData)
    response.headers.set('Cache-Control', 'public, s-maxage=300')
    
    return response
  } catch (error) {
    console.error('Account fetch error:', error)
    return NextResponse.json(
      { error: 'Failed to fetch account details' },
      { status: 500 }
    )
  }
}