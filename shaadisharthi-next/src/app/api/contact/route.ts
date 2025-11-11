import { NextRequest, NextResponse } from 'next/server';
import axios from 'axios';

export async function POST(req: NextRequest) {
  try {
    const body = await req.json();
    const res = await axios.post(`${process.env.BACKEND_URL}/contact`, body, {
      timeout: 5000,
      headers: { 'Content-Type': 'application/json' },
    });
    return NextResponse.json({ success: true, data: res.data }, { status: 200 });
  } catch (error: unknown) {
    let errorMessage = 'Failed to send message. Please try again.';
    let statusCode = 500;

    if (axios.isAxiosError(error)) {
      console.error('Contact API error:', error.message);
      errorMessage = error.response?.data?.error || error.message;
      statusCode = error.response?.status || 500;
    } else if (error instanceof Error) {
      console.error('Contact API error:', error.message);
      errorMessage = error.message;
    }
    return NextResponse.json(
      { success: false, error: errorMessage },
      { status: statusCode }
    );
  }
}