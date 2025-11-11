// app/api/auth/logout/route.ts
import { NextResponse } from 'next/server';
import { deleteSession } from '@/lib/session';

export async function POST() {
  try {
    await deleteSession();
    
    return NextResponse.json({ success: true });
  } catch (error) {
    console.error('Logout API error:', error);
    // Still return success as client will clear state anyway
    return NextResponse.json({ success: true });
  }
}