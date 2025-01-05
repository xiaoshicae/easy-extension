import { Avatar } from 'antd';
import React from 'react';
import { defaultColor, letterColorMap } from './colormap';

export type LetterAvatarProps = {
  letter?: string;
};

const LetterAvatar: React.FC<LetterAvatarProps> = ({ letter }) => {
  const color = letterColorMap[letter || 'C'] || defaultColor;
  return <Avatar style={{ backgroundColor: color }}>{letter}</Avatar>;
};

export default LetterAvatar;
