import { NextRequest, NextResponse } from 'next/server';
import axios from 'axios';

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const res = await axios.post(`${process.env.BACKEND_URL}/newsletter`, body, {
      timeout: 5000,
      headers: { 'Content-Type': 'application/json' },
    });
    return NextResponse.json({ success: true, data: res.data }, { status: 200 });
  } catch (error: unknown) {
    let statusCode = 500;
    if (axios.isAxiosError(error)) {
      console.error('Newsletter API error:', error.message);
      statusCode = error.response?.status || 500;
    } else if (error instanceof Error) {
      console.error('Newsletter API error:', error.message);
    }
    return NextResponse.json(
      { success: false, error: 'Failed to subscribe. Please try again.' },
      { status: statusCode }
    );
  }
}