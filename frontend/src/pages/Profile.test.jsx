// @vitest-environment jsdom
import {render,screen} from '@testing-library/react';
import {afterEach,describe,expect,it,vi} from 'vitest';
import '@testing-library/jest-dom/vitest';

const api=vi.hoisted(()=>({get:vi.fn(),put:vi.fn()}));
vi.mock('../services/api',()=>({default:api}));
import Profile from './Profile';

describe('profile department display',()=>{
  afterEach(()=>vi.resetAllMocks());

  it('resolves an assigned department ID to its name',async()=>{
    api.get.mockResolvedValue({data:{id:3,fullName:'Mahesh Savkar',email:'mahesh@example.com',role:'EMPLOYEE',departmentId:1,departmentName:'Computer'}});
    render(<Profile/>);
    expect(await screen.findByText('Computer')).toBeInTheDocument();
    expect(screen.queryByText('Department #1')).not.toBeInTheDocument();
  });

  it('shows Not assigned when the profile has no department',async()=>{
    api.get.mockResolvedValue({data:{id:4,fullName:'Rahul Sharma',email:'manager@example.com',role:'MANAGER',departmentId:null,departmentName:null}});
    render(<Profile/>);
    expect(await screen.findByText('Not assigned')).toBeInTheDocument();
  });
});
