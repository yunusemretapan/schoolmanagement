import { StatusBar } from 'expo-status-bar';
import { useCallback, useState } from 'react';
import { useAppearance } from './appearance/AppearanceProvider';
import { DesignShowcase } from './features/design/DesignShowcase';
import { StatusScreen } from './features/system/StatusScreen';

/** Two screens only: a state switch is enough until spec 0004 decides on navigation (plan 0002). */
export function Screens() {
  const { scheme } = useAppearance();
  const [screen, setScreen] = useState<'status' | 'showcase'>('status');
  const openShowcase = useCallback(() => setScreen('showcase'), []);
  const backToStatus = useCallback(() => setScreen('status'), []);
  return (
    <>
      {screen === 'showcase' ? (
        <DesignShowcase onBack={backToStatus} />
      ) : (
        <StatusScreen onOpenShowcase={openShowcase} />
      )}
      <StatusBar style={scheme === 'dark' ? 'light' : 'dark'} />
    </>
  );
}
