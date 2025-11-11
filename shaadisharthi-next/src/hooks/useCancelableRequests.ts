import { useRef, useEffect } from 'react';

export const useCancelableRequests = () => {
  const controllersRef = useRef<AbortController[]>([]);

  useEffect(() => {
    return () => {
      // Cleanup: cancel all pending requests on unmount
      controllersRef.current.forEach(controller => controller.abort());
      controllersRef.current = [];
    };
  }, []);

  const createController = () => {
    const controller = new AbortController();
    controllersRef.current.push(controller);
    return controller;
  };

  const cancelAll = () => {
    controllersRef.current.forEach(controller => controller.abort());
    controllersRef.current = [];
  };

  return { createController, cancelAll };
};